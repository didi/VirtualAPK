package com.didi.virtualapk.aapt

/**
 * Class to edit aapt-generated resources.arsc file
 */
public class ArscEditor extends AssetEditor {
    /*      Arsc struct
     *  +-----------------------+
     *  | Table Header          |
     *  +-----------------------+
     *  | Res string pool       |
     *  +-----------------------+
     *  | Package Header        | <-- rewrite entry 1: package id
     *  +-----------------------+
     *  | Type strings          |
     *  +-----------------------+
     *  | Key strings           |
     *  +-----------------------+
     *  | DynamicRefTable chunk | <-- insert entry (for 5.0+)
     *  +-----------------------+
     *  | Type spec             |
     *  |                  * N  |
     *  | Type info  * M        | <-- rewrite entry 2: entry value
     *  +-----------------------+
     */

    private static final boolean DEBUG_NOISY = false // log verbose

    private static def LIBRARY_HEADER_SIZE = 0x0C
    private static def LIBRARY_ENTRY_SIZE = 260 // packageId(4), packageName(256)
    private static def LIBRARY_CHUNK_SIZE = 272 // ResTable_lib_header & ResTable_lib_entry
    private static def TABLE_SIZE_POS = 4

    private int mTableConfigSize = 52 // sizeof(ResTable_config)

    ArscEditor(File file, def v) {
        super(file, v)
        if (version != null && version.major >= 24) {
            mTableConfigSize = 56
        }
    }

    /**
     * Slice asset package and reset resource package ids
     * @param pp new resource package id
     * @param idMaps
     * @param retainedTypes the resource types to retain
     * @return
     */
    def slice(int pp, Map idMaps, Map libRefTable, List retainedTypes) {
        def t = readTable()

        def retainedTypeSpecs = []
        def retainedStringIds = []
        def retainedTypeIds = []
        def retainedKeyIds = []
        def retainedEntries = []
        def libPackageIds = []

        if (t.typeList.specs.size() == 0) {
            println "\t -- There was no res."
            return
        }

        // Ensure there is an `attr' typeSpec
        if (retainedTypes[0].id == Aapt.ID_NO_ATTR) { // attr type id is always at first
            def attrSpec = t.typeList.specs[0]
            attrSpec.entryCount = 0
            attrSpec.configs = []
            attrSpec.flags = []
            attrSpec.header.size = attrSpec.header.headerSize // id(1) res0(1) res1(2) entryCount(4)
            retainedTypeIds.add(attrSpec.id - 1)
            retainedTypeSpecs.add(attrSpec)
            println "\t -- There was no attr."
        }

        def index = 0
        while (index < t.stringPool.styleCount) {
            retainedStringIds.add(index++)
        }

        // Create the mapping of type ids
        LinkedHashMap<Object, Integer> typeIdMap = new LinkedHashMap<>()
        t.typeList.specs.eachWithIndex { it, i ->
            typeIdMap.put(it.id.intValue(), i)
        }

        // Filter typeSpecs
        retainedTypes.each {
            if (it.id == Aapt.ID_DELETED) {
                // TODO: Add empty entry to default config
                throw new UnsupportedOperationException("No support deleting resources on lib.* now")
            }

            if (it.id == Aapt.ID_NO_ATTR) {
                return
            }

            def specIndex = typeIdMap.get(it.id)
            def ts = t.typeList.specs[specIndex]
            def es = it.entries
            def newEntryCount = es.size()
            def d = (ts.entryCount - newEntryCount) * 4
            ts.entryCount = newEntryCount
            // Filter flags
            def flags = []
            es.each { e ->
                def flag = (e.id == Aapt.ID_DELETED) ? 0 : ts.flags[e.id]
                flags.add(flag)
            }
            ts.flags = flags
            ts.header.size -= d
            ts.id = retainedTypeSpecs.size() + 1
            // Filter config entries
            def configs = []
            ts.configs.each {
                def entries = []
                def offsets = []
                int offset = 0
                def emptyCount = 0
                es.each { e ->
                    if (e.id == Aapt.ID_DELETED) {
                        // TODO: Add empty entry to default config
                        throw new UnsupportedOperationException("No support deleting resources on lib.* now")
                    }

                    def entry = it.entries[e.id]
                    if (entry == null) {
                        throw new Exception("Missing entry at ${e} on ${it}!")
                    }

                    entries.add(entry)
                    if (entry.isEmpty()) {
                        offsets.add(-1)
                        emptyCount++
                        return
                    }

                    def ename = new String(t.keyStringPool.strings[entry.key]).replaceAll('\\.', '_')
                    if (e.name != ename) {
                        throw new Exception("Required entry '${e.name}' but got '$ename', This " +
                                "is seems to unsupport the buildToolsRevision: ${version}.")
                    }

                    offsets.add(offset)
                    offset += entry.allSize
                    if (!retainedKeyIds.contains(entry.key)) retainedKeyIds.add(entry.key)
                    retainedEntries.add(entry)
                    int dataType
                    if (entry.value != null) {
                        // Reset entry ids
                        dataType = entry.value.dataType
                        if (dataType == ResValueDataType.TYPE_STRING) {
                            // String reference
                            def oldId = entry.value.data
                            def newId = retainedStringIds.indexOf(oldId)
                            if (newId < 0) {
                                retainedStringIds.add(oldId)
                                newId = retainedStringIds.size() - 1
                            }
                            entry.value.data = newId
                        } else if (dataType == ResValueDataType.TYPE_REFERENCE) {
                            def id = idMaps.get(entry.value.data)
                            if (id != null) {
                                if (DEBUG_NOISY) println "\t -- map ResTable_entry.value: " +
                                        "${String.format('0x%08x', entry.value.data)} -> " +
                                        "${String.format('0x%08x', id)}"
                                entry.value.data = id
                            }
                        }
                    } else if (entry.maps != null) {
                        // Reset entry parent
                        def id = idMaps.get(entry.parent)
                        if (id != null) {
                            if (DEBUG_NOISY) println "\t -- map ResTable_map_entry.parent: " +
                                    "${String.format('0x%08x', entry.parent)} -> " +
                                    "${String.format('0x%08x', id)}"
                            entry.parent = id
                        }
                        entry.maps.each {
                            // Reset map ids
                            id = idMaps.get(it.name)
                            if (id != null) {
                                if (DEBUG_NOISY) println "\t -- map ResTable_map.name: " +
                                        "${String.format('0x%08x', it.name)} -> " +
                                        "${String.format('0x%08x', id)}"
                                it.name = id
                            }
                            dataType = it.value.dataType
                            if (dataType == ResValueDataType.TYPE_STRING) {
                                // String reference
                                def oldId = it.value.data
                                def newId = retainedStringIds.indexOf(oldId)
                                if (newId < 0) {
                                    retainedStringIds.add(oldId)
                                    newId = retainedStringIds.size() - 1
                                }
                                it.value.data = newId
                            } else if (dataType == ResValueDataType.TYPE_REFERENCE) {
                                id = idMaps.get(it.value.data)
                                if (id != null) {
                                    if (DEBUG_NOISY) println "\t -- map ResTable_map.value: " +
                                            "${String.format('0x%08x', it.value.data)} -> " +
                                            "${String.format('0x%08x', id)}"
                                    it.value.data = id

                                    int pid = (id >> 24)
                                    if (pid != 0x7f && pid != 0x01 && pid != pp) {
                                        libPackageIds.add(pid)
                                    }
                                }
                            }
                        }
                    }
                }

                if (emptyCount == ts.entryCount) return

                it.entries = entries
                it.entryOffsets = offsets
                it.entryCount = ts.entryCount
                it.entriesStart -= d
                it.header.size -= d + it.entriesSize - offset
                it.id = ts.id
                configs.add(it)
            }

            ts.configs = configs
            retainedTypeSpecs.add(ts)
            retainedTypeIds.add(it.id - 1)
        }

        // Reset entry keys (reference to keyStringPool index)
        def keyMaps = [:]
        retainedKeyIds.eachWithIndex { key, newKey ->
            keyMaps.put(key, newKey)
        }
        retainedEntries.each { e ->
            e.key = keyMaps[e.key]
        }
        t.typeList.specs = retainedTypeSpecs


        // Filter string pools
        filterStringPool(t.stringPool, retainedStringIds)
        filterStringPool(t.typeStringPool, retainedTypeIds)
        filterStringPool(t.keyStringPool, retainedKeyIds)

        // Add dynamic ref table for 5.0+
        def lib = t.typeList.lib
        if (lib == null) {
            lib = [:]
            lib.header = [:]
            lib.header.type = ResType.RES_TABLE_LIBRARY_TYPE
            lib.header.headerSize = LIBRARY_HEADER_SIZE
            lib.header.size = LIBRARY_HEADER_SIZE + LIBRARY_ENTRY_SIZE
            lib.count = 1
            lib.entries = []
            t.typeList.lib = lib
        } else {
            lib.count ++
            lib.header.size += LIBRARY_ENTRY_SIZE
        }
        def libEntry = [:]
        libEntry.packageId = pp
        libEntry.packageName = t.package.name
        lib.entries.add(libEntry)

        // more dynamic ref table from related libraries
        libPackageIds.each { pid ->
            def pname = libRefTable[pid]
            if (pname == null) {
                def err = "Failed to resolve package: ${String.format('0x%02x', pid)}\n"
                libRefTable.each { id, name ->
                    err += "  [${String.format('0x%02x', id)}] -> $name\n"
                }
                throw new RuntimeException(err)
            }

            lib.count ++
            lib.header.size += LIBRARY_ENTRY_SIZE
            lib.entries.add([packageId: pid,
                             packageName: getUtf16String(pname, 256)])
        }

        // Reset sizes & offsets
        int size = lib.header.size
        t.typeList.specs.each { ts ->
            size += ts.header.size
            ts.configs.each {
                size += it.header.size
            }
        }
        size += t.typeStringPool.header.size + t.keyStringPool.header.size +
                t.package.header.headerSize
        t.package.header.size = size
        size += t.stringPool.header.size + t.header.headerSize
        t.header.size = size
        t.package.keyStrings = t.package.typeStrings + t.typeStringPool.header.size
        t.package.lastPublicType = t.typeStringPool.strings.size()
        t.package.lastPublicKey = t.keyStringPool.strings.size()

        // Rewrite
        t.package.id = pp
        setLength(t.header.size)
        seek(0)
        writeTable(t)

        if (DEBUG_NOISY) dumpTable()

        close()
    }

    /**
     * Reset resource package ids
     * @param pp new resource package id
     * @param idMaps
     * @return
     */
    def reset(int pp, Map idMaps) {
        def table = [:]
        table.header = readChunkHeader()
        assert (table.header.type == ResType.RES_TABLE_TYPE)

        table.packageCount = readInt()

        // Skip ResStringPool
        def packageIdPos = table.header.headerSize
        def strChunk = this.readChunkHeader()
        assert (strChunk.type == ResType.RES_STRING_POOL_TYPE)

        packageIdPos += strChunk.size
        seek(packageIdPos)

        // Enter ResTable_package
        def pkgChunk = this.readChunkHeader()
        assert (pkgChunk.type == ResType.RES_TABLE_PACKAGE_TYPE)
        def pkgSizePos = tellp() - 4 // for adding size after insert dynamicRefTable

        // Rewrite package id
        writeInt(pp)
        def packageName = readBytes(256)
        skip(20) // skip other fields of ResTable_package

        // Skip typeStrings
        def chunk = readChunkHeader()
        assert (chunk.type == ResType.RES_STRING_POOL_TYPE)
        skip(chunk.size - CHUNK_HEADER_SIZE)

        // Skip keyStrings
        chunk = readChunkHeader()
        assert (chunk.type == ResType.RES_STRING_POOL_TYPE)
        skip(chunk.size - CHUNK_HEADER_SIZE)

        def dynamicRefPos = tellp()
        def offset = dynamicRefPos
        def type = readTableType()
        while (offset < table.header.size) {
//            println "-- type: $type"
            if (type.isConfig) { // ResTable_type
                def entryOffsets = []
                for (int i = 0; i < type.entryCount; i++) {
                    entryOffsets[i] = readInt()
                }
                def entriesStart = tellp()
                for (int i = 0; i < type.entryCount; i++) {
                    def entryOffset = entryOffsets[i]
                    if (entryOffset == ResTableType.NO_ENTRY) continue

                    def entryPos = entriesStart + entryOffset
                    if (table.header.size - entryPos < 16) break // requires 16 bytes at least
                    seek(entryPos)
                    def entry = _readTableEntry()
                    if (entry.flags == 0) {
                        checkToRewriteTypedValueId(pp, idMaps)// Res_value
                    } else if (entry.flags & ResTableEntry.FLAG_COMPLEX) { // ResTable_mapEntry
                        checkToRewritePackageId(pp, idMaps) // mapEntry.parent
                        def count1 = readInt()
                        def count = count1 & 0x00ffff // fix aapt v22 bug?
                        for (int j = 0; j < count; j++) {
                            checkToRewritePackageId(pp, idMaps) // map.name
                            checkToRewriteTypedValueId(pp, idMaps) // map.value
                        }
                    }
                }
            }
            offset += type.header.size
            seek(offset)
            type = readTableType()
        }

        // for 5.0+
        insertDynamicRefTable(packageName, pp, dynamicRefPos)
        // Add table size
        seek(TABLE_SIZE_POS)
        writeInt(table.header.size + LIBRARY_CHUNK_SIZE)
        seek(pkgSizePos)
        writeInt(pkgChunk.size + LIBRARY_CHUNK_SIZE)

        close()
    }

    /** Read all data of resources.arsc */
    def readTable() {
        def header = readChunkHeader()
        assert (header.type == ResType.RES_TABLE_TYPE)

        def t = [:]
        t.header = header
        t.packageCount = readInt()
        t.stringPool = readStringPool()
        t.package = readPackage()
        t.typeStringPool = readStringPool()
        t.keyStringPool = readStringPool()
        t.typeList = readTypeList()
        return t
    }
    /** Write all data of resources.arsc */
    def writeTable(t) {
        writeChunkHeader(t.header)
        writeInt(t.packageCount)
        writeStringPool(t.stringPool)
        writePackage(t.package)
        writeStringPool(t.typeStringPool)
        writeStringPool(t.keyStringPool)
        writeTypeList(t.typeList)
    }

    /** Read struct ResTable_package */
    def readPackage() {
        def header = readChunkHeader()
        assert (header.type == ResType.RES_TABLE_PACKAGE_TYPE)

        def p = [:]
        p.header = header
        p.id = readInt()
        p.name = readBytes(256)
        p.typeStrings = readInt()
        p.lastPublicType = readInt()
        p.keyStrings = readInt()
        p.lastPublicKey = readInt()
        p.typeIdOffset = readInt()
        return p
    }
    /** Write struct ResTable_package */
    def writePackage(p) {
        writeChunkHeader(p.header)
        writeInt(p.id)
        writeBytes(p.name)
        writeInt(p.typeStrings)
        writeInt(p.lastPublicType)
        writeInt(p.keyStrings)
        writeInt(p.lastPublicKey)
        writeInt(p.typeIdOffset)
    }

    /** Read 1 x ResTable_lib + (ResTable_typeSpec + ResTable_type x M) x N */
    def readTypeList() {
        def offset = tellp()
        def length = length()
        def lib = null // ResTable_lib
        def specs = [] // ResTable_typeSpec
        def currTypeSpec = null
        while (offset < length) {
            def tt = [:]
            tt.header = readChunkHeader()
            switch (tt.header.type) {
                case ResType.RES_TABLE_TYPE_SPEC_TYPE:
                    tt.id = readByte()
                    tt.res0 = readByte()
                    tt.res1 = readShort()
                    tt.entryCount = readInt()
                    tt.flags = []
                    for (int i = 0; i < tt.entryCount; i++) {
                        tt.flags[i] = readInt()
                    }
                    currTypeSpec = tt
                    specs.add(tt)
                    break
                case ResType.RES_TABLE_TYPE_TYPE:
                    tt.id = readByte()
                    tt.res0 = readByte()
                    tt.res1 = readShort()
                    tt.entryCount = readInt()
                    tt.entriesStart = readInt()
                    tt.config = readTableConfig()
                    tt.entryOffsets = []
                    tt.entries = []
                    for (int i = 0; i < tt.entryCount; i++) {
                        tt.entryOffsets.add(readInt())
                    }
                    int start = offset + tt.entriesStart
                    tt.entriesSize = 0
                    for (int i = 0; i < tt.entryCount; i++) {
                        int pos = tt.entryOffsets[i]
                        if (pos == -1) {
                            tt.entries.add([:])
                            continue
                        }
                        pos += start
                        seek(pos)
                        def entry = readTableEntry()
                        entry.allSize = tellp() - pos
                        tt.entries.add(entry)
                        tt.entriesSize += entry.allSize
                    }
                    if (currTypeSpec.configs == null) currTypeSpec.configs = []
                    currTypeSpec.configs.add(tt)
                    break
                case ResType.RES_TABLE_LIBRARY_TYPE:
                    tt.count = readInt()
                    tt.entries = []
                    for (int i = 0; i < tt.count; i++) {
                        def entry = [:]
                        entry.packageId = readInt()
                        entry.packageName = readBytes(256)
                        tt.entries.add(entry)
                    }
                    lib = tt
                    break
                default:
                    println "!!!Unkown type: ${String.format('0x%04x', tt.header.type)}"
                    seek(tellp() - 8)
                    dumpBytes(32)
                    assert(false)
            }
            offset += tt.header.size
            seek(offset)
        }
        return [specs:specs, lib:lib]
    }
    /** Write 1 x ResTable_lib + (ResTable_typeSpec + ResTable_type x M) x N */
    def writeTypeList(tl) {
        if (tl.lib) {
            // ResTable_lib (5.0+)
            writeChunkHeader(tl.lib.header)
            writeInt(tl.lib.count)
            tl.lib.entries.each {
                writeInt(it.packageId)
                writeBytes(it.packageName)
            }
        }
        tl.specs.each { ts ->
            // ResTable_typeSpec
            writeChunkHeader(ts.header)
            writeByte(ts.id)
            writeByte(ts.res0)
            writeShort(ts.res1)
            writeInt(ts.entryCount)
            ts.flags.each {
                writeInt(it ?: 0)
            }
            ts.configs.each { c ->
                // ResTable_type
                writeChunkHeader(c.header)
                writeByte(c.id)
                writeByte(c.res0)
                writeShort(c.res1)
                writeInt(c.entryCount)
                writeInt(c.entriesStart)
                writeTableConfig(c.config)
                c.entryOffsets.each {
                    writeInt(it)
                }
                c.entries.each { e ->
                    if (e.isEmpty()) return
                    writeTableEntry(e)
                }
            }
        }
    }

    /** Read struct ResTable_entry */
    def _readTableEntry() {
        def e = [:]
        e.size = readShort()
        e.flags = readShort()
        e.key = readInt()
        return e
    }
    /** Read struct ResTable_entry or ResTable_map_entry */
    def readTableEntry() {
        def e = _readTableEntry()
        if (e.flags & ResTableEntry.FLAG_COMPLEX) {
            // ResTable_map_entry
            e.parent = readInt()
            e.count = readInt()
            e.maps = []
            for (int i = 0; i < e.count; i++) {
                e.maps.add(readTableMap())
            }
        } else {
            e.value = readResValue()
        }
        return e
    }
    /** Write struct ResTable_entry or ResTable_map_entry */
    def writeTableEntry(e) {
        writeShort(e.size)
        writeShort(e.flags)
        writeInt(e.key)
        if (e.maps != null) {
            writeInt(e.parent)
            writeInt(e.count)
            e.maps.each {
                writeTableMap(it)
            }
        } else {
            writeResValue(e.value)
        }
    }

    /** Read struct ResTable_map */
    def readTableMap() {
        def m = [:]
        m.name = readInt()
        m.value = readResValue()
        return m
    }
    /** Write struct ResTable_map */
    def writeTableMap(m) {
        writeInt(m.name)
        writeResValue(m.value)
    }

    /** Read struct ResTable_config */
    def readTableConfig() {
        def c = [:]
//        c.size = readInt()
//        c.imsi = [:]
//        c.imsi.mcc = readShort()
//        c.imsi.mnc = readShort()
//        c.locale = [:]
//        c.locale.language = readBytes(2)
//        c.locale.country = readBytes(2)
//        c.screenType = [:]
//        c.screenType.orientation = readByte()
//        c.screenType.touchscreen = readByte()
//        c.screenType.density = readShort()
//        c.input = [:]
//        c.input.keyboard = readByte()
//        c.input.navigation = readByte()
//        c.input.inputFlags = readByte()
//        c.input.inputPad0 = readByte()
//        c.screenSize = [:]
//        c.screenSize.screenWidth = readShort()
//        c.screenSize.screenHeight = readShort()
//        c.version = [:]
//        c.version.sdkVersion = readShort()
//        c.version.minorVersion = readShort()
//        c.screenConfig = [:]
//        c.screenConfig.screenLayout = readByte()
//        c.screenConfig.uiMode = readByte()
//        c.screenConfig.smallestScreenWidthDp = readShort()
//        c.screenSizeDp = [:]
//        c.screenSizeDp.screenWidthDp = readShort()
//        c.screenSizeDp.screenHeightDp = readShort()
//        c.localeScript = readBytes(4)
//        c.localeVariant = readBytes(8)
//        c.screenConfig2 = [:]
//        c.screenConfig2.screenLayout2 = readByte()
//        c.screenConfig2.screenConfigPad1 = readByte()
//        c.screenConfig2.screenConfigPad2 = readShort()
        c.ignored = readBytes(mTableConfigSize)
        return c
    }
    /** Write struct ResTable_config */
    def writeTableConfig(c) {
        writeBytes(c.ignored)
    }

    /** Read struct ResTable_typeSpec or ResTable_type */
    private def readTableType() {
        def type = [:]
        type.header = readChunkHeader()
        if (type.header.type == ResType.RES_TABLE_TYPE_SPEC_TYPE) {
            type.isConfig = false
            skip(4) // id(1), res0(1), res1(2)
            type.entryCount = readInt()
        } else if (type.header.type == ResType.RES_TABLE_TYPE_TYPE) {
            type.isConfig = true
            skip(4) // id(1), res0(1), res1(2)
            type.entryCount= readInt()
            type.entriesStart = readInt()
            skip(mTableConfigSize) // ResTable_type.config: struct ResTable_config
        }
        return type
    }

    private def insertDynamicRefTable(byte[] packageName, int pp, long pos) {
        // Cut data after pos to clip channel
        clipLaterData(pos)

        // write ResTable_lib_header
        seek(pos)
        writeShort(ResType.RES_TABLE_LIBRARY_TYPE) // header.type (2)
        writeShort(LIBRARY_HEADER_SIZE) // header.headerSize (2)
        writeInt(LIBRARY_CHUNK_SIZE) // header.size (4)
        writeInt(0x1) // count (4)
        // write ResTable_lib_entry
        writeInt(pp) // packageId (4)
        writeBytes(packageName) // packageName (256)

        // Paste data from clip channel
        pasteLaterData(pos)
    }

    private static def getConfigName(c) {
        def s = ''
//        switch (c.screenType.density) {
//            case 160: s += 'mdpi'; break
//            case 240: s += 'hdpi'; break
//            case 320: s += 'xhdpi'; break
//            case 480: s += 'xxhdpi'; break
//            case 640: s += 'xxxhdpi'; break
//        }
//        if (c.screenSizeDp.screenWidthDp != 0) s += "w${c.screenSizeDp.screenWidthDp}dp"
//        if (c.version.sdkVersion != 0) s += "-v${c.version.sdkVersion}"
        if (s == '') s = '(default)'
        return s
    }

    private def dumpTable() {
        seek(0)
        def t = readTable()
        dumpTable(t)
    }

    /** Dump table as `aapt d resources' */
    private def dumpTable(t) {
        println "String Pool:"
        dumpStringPool(t.stringPool)
        println "Type String Pool:"
        dumpStringPool(t.typeStringPool)
        println "Key String Pool:"
        dumpStringPool(t.keyStringPool)

        def pname = getUtf8String(t.package.name)
        def pid = t.package.id << 24
        def pidStr = "0x${Integer.toHexString(t.package.id)}"

        println "Package Groups (${t.packageCount})"
        println "Package Group 0 id=$pidStr packageCount=${t.packageCount} name=$pname"
        def lib = t.typeList.lib
        if (lib != null) {
            println "  DynamicRefTable entryCount=${lib.count}"
            lib.entries.each{ e ->
                println "    0x${Integer.toHexString(e.packageId)} -> " +
                        "${getUtf8String(e.packageName)}"
            }
            println ''
        }

        println "  Package 0 id=$pidStr name=$pname"

        def keyId = 0
        t.typeList.specs.each { ts ->
            if (ts.configs == null) return
            def configCount = ts.configs.size()
            if (configCount == 0) return
            def entryCount = ts.entryCount
            if (entryCount == 0) return
            def type = new String(t.typeStringPool.strings[ts.id - 1])
            println "    type ${ts.id - 1} configCount=$configCount entryCount=$entryCount"
            for (int ei = 0; ei < entryCount; ei++) {
                def id = Integer.toHexString(pid | (ts.id << 16) | ei)
                def keyBuff = t.keyStringPool.strings[keyId]
                def key = keyBuff == null ? 'null' : new String(keyBuff)
                print "      spec resource 0x$id $pname:$type/$key: flags=0x"
                println String.format('%08x', ts.flags[ei])
                keyId++
            }
            ts.configs.each {
                println "      config ${getConfigName(it.config)}:"
                it.entries.eachWithIndex { e, ei ->
                    if (e.key == null) return

                    def id = Integer.toHexString(pid | (ts.id << 16) | ei)
                    def keyBuff = t.keyStringPool.strings[e.key]
                    def key = keyBuff == null ? 'null' : new String(keyBuff)
                    print "        resource 0x$id $pname:$type/$key: "
                    if (e.value != null) {
                        print String.format('t=0x%02x d=0x%08x (s=0x%04x r=0x%02x)',
                                e.value.dataType, e.value.data, e.value.size, e.value.res0)
                    } else {
                        print '<bag>'
                    }
                    if (e.flags & ResTableEntry.FLAG_PUBLIC) print ' (PUBLIC)'
                    println ''
                }
            }
        }
    }
}
