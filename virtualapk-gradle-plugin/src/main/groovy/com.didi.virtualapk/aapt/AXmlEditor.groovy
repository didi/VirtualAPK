package com.didi.virtualapk.aapt

/**
 * Class to edit aapt-generated hex xml file
 */
public class AXmlEditor extends AssetEditor {

    private static final ATTR_BEFORE_ID_LENGTH = 16

    AXmlEditor(final File file) {
        super(file)
    }

    // To support plugin JNI, we carry the ABIs flag to extract the exactly JNIs
    // under the supported ABI at runtime. (#87, #79)
    //
    // In addition, we find that if `addAssetPath` with an non-resources apk to `AssetManager`,
    // Small will crash on the asset manager's `getPooledString` method under android M. (#62)
    // So we also carry a flag to specify whether the plugin apk has resources.
    //
    // The above flag will be merged into an integer and write to `platformBuildVersion`
    def setSmallFlags(final int flags) {
        def xml = readChunkHeader()
        if (xml.type != ResType.RES_XML_TYPE) {
            return
        }

        def sp = readStringPool()
        byte[] targetBytes = [ // platformBuildVersionCode
                'p',0,'l',0,'a',0,'t',0,'f',0,'o',0,'r',0,'m',0,'B',0,'u',0,'i',0,'l',0,'d',0,
                'V',0,'e',0,'r',0,'s',0,'i',0,'o',0,'n',0,'C',0,'o',0,'d',0,'e',0 ]
        int targetIndex = -1
        final int N = sp.stringCount
        for (int i = 0; i < N; i++) {
            def bytes = sp.strings[i]
            if (Arrays.equals(bytes, targetBytes)) {
                targetIndex = i
                break
            }
        }

        if (targetIndex == -1) {
            return
        }

        while (tellp() < xml.size) {
            def chunk = readChunkHeader()
            if (chunk.type != ResType.RES_XML_START_ELEMENT_TYPE) {
                skipChunk(chunk)
                continue
            }

            // The first element: <manifest ...
            def node = readNode()
            for (int i = 0; i < node.attributeCount; i++) {
                skip(4)
                int nameIndex = readInt()
                if (nameIndex == targetIndex) { // platformBuildVersionCode
                    skip(8)

                    final int versionCode = readInt()

                    seek(tellp() - 4)

                    // The flag bits are:
                    //  F    F    F    F    F    F    F    F
                    // 1111 1111 1111 1111 1111 1111 1111 1111
                    // ^^^^ ^^^^ ^^^^ ^^^^ ^^^^
                    //       ABI Flags (20)
                    //                          ^
                    //                 nonResources Flag (1)
                    //                           ^^^ ^^^^ ^^^^
                    //                     platformBuildVersionCode (11) => MAX=0x7FF=4095
                    final int newFlag = (flags << 11) | versionCode
                    writeInt(newFlag)
                    close()
                    return true
                } else {
                    skip(12)
                }
            }

            break
        }

        close()
        return false
    }

    def setPackageId(final int pp, final Map idMaps) {
        def xml = readChunkHeader()
        if (xml.type != ResType.RES_XML_TYPE) {
            return false
        }

        setPackageIdRecursive(pp, idMaps, xml.size)
        close()
        return edited
    }

    private def setPackageIdRecursive(final int pp, final Map idMaps, final long size) {
        if (tellp() >= size) {
            return
        }

        def chunk = readChunkHeader()
        if (chunk.type == ResType.RES_XML_RESOURCE_MAP_TYPE) {
            def idCount = (chunk.size - chunk.headerSize) / 4
            for (int i = 0; i < idCount; i++) {
                checkToRewritePackageId(pp, idMaps)
            }
        } else if (chunk.type == ResType.RES_XML_START_ELEMENT_TYPE) {
            // Parse element, reset package id
            def node = readNode()
            for (int i = 0; i < node.attributeCount; i++) {
                skip(ATTR_BEFORE_ID_LENGTH)
                checkToRewritePackageId(pp, idMaps)
            }
        } else {
            skip(chunk.size - CHUNK_HEADER_SIZE)
        }

        setPackageIdRecursive(pp, idMaps, size)
    }

    /** Read struct ResXMLTree_node and ResXMLTree_attrExt */
    private def readNode() {
        def node = [:]
        // Skip struct ResXMLTree_node: lineNumber(4), comment(4) and part of struct
        // ResXMLTree_attrExt: ns(4), name(4), attributeStart(2), attributeSize(2)
        skip(20)
        node.attributeCount = readShort()
        // skip tail of struct ResXMLTree_attrExt: idIndex(2), classIndex(2), styleIndex(2)
        skip(6)
        return node
    }

    def createAndroidManefist(Map options) {
        def size = 0
        def xml = [
            header: [type: ResType.RES_XML_TYPE, headerSize: 8, size: size],
            stringPool: []
        ]
    }

    public void dumpXmlTree() {
        def xml = readXmlTree()
        // TODO
    }

    private def readXmlTree() {
        def xml = [:]
        xml.header = readChunkHeader()
        assert (xml.header.type == ResType.RES_XML_TYPE)

        def size = xml.header.size

        println "pos1: ${tellp()}"

        xml.stringPool = readStringPool()
        println xml.stringPool.flags

        dumpStringPool(xml.stringPool)

        readChunks(xml)
//        println xml
//        xml.namespaces.each {
//            println it
//        }
//        println xml.namespaces
    }

    private def readChunks(xml) {
        def size = length()
        def currNamespace = null
        def currNode = null
        def pos
        while ((pos = tellp()) < size) {
            def header = readChunkHeader()
            println header
            switch (header.type) {
                case ResType.RES_XML_RESOURCE_MAP_TYPE:
                    def mapCount = (header.size - header.headerSize) / 4
                    def maps = []
                    for (int i = 0; i < mapCount; i++) {
                        maps.add(readInt())
                    }
                    xml.attrMap = [header: header, ids: maps]
                    break
                case ResType.RES_XML_START_NAMESPACE_TYPE:
                    def ns = [header: header]
                    ns.startLine = readInt()
                    ns.headComment = readInt()
                    ns.prefix = readInt()
                    ns.uri = readInt()
                    ns.nodes = []
                    def nss = xml.namespaces
                    if (nss == null) {
                        xml.namespaces = nss = []
                    }
                    nss.add(ns)
                    ns.currNode = ns
                    currNamespace = ns
                    break
                case ResType.RES_XML_START_ELEMENT_TYPE:
                    def node = [:]
                    node.startLine = readInt()
                    node.headComment = readInt()
                    node.ns = readInt()
                    node.name = readInt()
                    node.attributeStart = readShort()
                    node.attributeSize = readShort()
                    node.attributeCount = readShort()
                    node.idIndex = readShort()
                    node.classIndex = readShort()
                    node.styleIndex = readShort()
                    node.attrs = []
                    for (int i = 0; i < node.attributeCount; i++) {
                        node.attrs.add(readAttribute())
                    }
                    node.nodes = []
                    def parent = currNamespace.currNode
                    println "- parent: ${parent}"
                    parent.nodes.add(node)
                    println "- parent: ${parent}"
                    node.parent = parent
                    println "- 333"
                    currNamespace.currNode = node
                    println "- 444"
                    break
                case ResType.RES_XML_END_NAMESPACE_TYPE:
                    currNamespace.endLine = readInt()
                    currNamespace.tailComment = readInt()
                    skip(8) // Bypass ns and name, ignore tag check
                    currNamespace = null
                    break
                case ResType.RES_XML_END_ELEMENT_TYPE:
                    println "- 555"
                    currNode.endLine = readInt()
                    currNode.tailComment = readInt()
                    skip(8) // Ignore tag check
                    println "- 666 $currNode"
                    currNode = currNode.parent
                    println "- 777 $currNode"
                    break
                default:
//                    pos += header.size - CHUNK_HEADER_SIZE
                    println "-- left: ${header.size + pos - tellp()}"
                    dumpBytes(header.size + pos - tellp())
//                    pos = tellp()
//                    seek(pos)
                    break
            }
        }
    }

    private def readAttribute() {
        def at = [:]
        at.ns = readInt()
        at.name = readInt()
        at.rawValue = readInt()
        at.typedValue = readResValue()
    }
}
