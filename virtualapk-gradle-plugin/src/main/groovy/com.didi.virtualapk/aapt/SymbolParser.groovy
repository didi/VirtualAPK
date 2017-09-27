package com.didi.virtualapk.aapt

/**
 * Class to parse aapt-generated text symbols file (intermediates/symbols/R.txt)
 */
public final class SymbolParser {

    public static final class Entry {
        public final String type
        public final String name

        public String getKey() {
            return "$type/$name"
        }

        Entry(final type, final name) {
            this.type = type
            this.name = name
        }

        @Override
        String toString() {
            return this.key
        }

        @Override
        boolean equals(final Object obj) {
            if (this.is(obj)) {
                return true
            }

            if (obj instanceof Entry) {
                Entry e = (Entry) obj
                return e.type.equals(type) && e.name.equals(name)
            }

            return false
        }

        @Override
        int hashCode() {
            return this.key.hashCode()
        }
    }

    /**
     * Get declare of one line
     *
     * @param s
     *            e.g. 'int anim abc_fade_in 0x7f050000'
     * @return e.g. 'int anim abc_fade_in'
     */
    public static String getResourceDeclare(final String s) {
        def arr = s.toCharArray()
        def find = 0
        def i = 0
        for (; i < arr.length; i++) {
            def c = arr[i]
            if (c == ' ') find++
            if (find == 3) break // skip 3 spaces
        }

        return s.substring(0, i)
    }

    /**
     * Get entry data of one line
     *
     * @param str
     *            the line text
     * @param needsId
     * @return entry map, e.g. [type:string, typeId:6, entryId:21, key:hello, id:0x7f060015]
     */
    public static Map<String, ?> getResourceEntry(String str) {
        if (str == '') return null

        final def tokenizer = new StringTokenizer(str)
        final def vtype = tokenizer.nextToken()     // value type (int or int[])
        final def type = tokenizer.nextToken()      // resource type (attr/string/color etc.)
        final def key = tokenizer.nextToken()
        final def idStr = tokenizer.nextToken('\r\n').trim()

        if (type == 'styleable') {
            // Styleables won't be compiled to resources.arsc file but saved in `R.java',
            // it just stores the 'attr/*' ids which are used to access values from AttributeSet.
            // As example:
            //   'int[] styleable MyTextView { 0x7f010055, 0x7f010056 }'
            //   'int styleable MyTextView_label 0'
            //   'int styleable MyTextView_label2 1'
            //
            //   TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyTextView);
            //   String label = ta.getString(R.styleable.MyTextView_label);
            //
            // The id 0x7f010055 and 0x7f010056 refer to 'attr/label' and 'attr/label2', so the
            // reading TypedArray contains the values of them.
            // The id 0 and 1 specify the value location in TypedArray.
            def e = [vtype: vtype, type: type, key: key, idStr: idStr, isStyleable: true]
            def idLen = idStr.length()
            if (idLen > 4) { // hereby, vtype must be int[] and the idStr is not empty as '{ }'
                def ids = idStr.substring(2, idStr.length() - 2) // bypass '{ ' and ' }'
                e.idStrs = ids.split(', ') as List<String>
            }
            return e
        }

        int typeId = Integer.parseInt(idStr.substring(4, 6), 16)
        int entryId = Integer.parseInt(idStr.substring(6), 16)
        int id = Integer.decode(idStr)
        return [vtype: vtype, type: type, key: key,
                typeId: typeId, entryId: entryId, idStr: idStr, id: id, isStyleable: false]
    }

    /**
     * Get entries data of each line
     *
     * @param file
     * @return
     */
    public static Map<String, Map<String, ?>> getResourceEntries(final File file) {
        def es = [:]
        if (!file.exists()) {
            return es
        }

        file.eachLine { str ->
            def entry = getResourceEntry(str)
            if (entry == null) return
            es.put("${entry.type}/${entry.key}", entry)
        }
        return es
    }

    public static void collectResourceKeys(File file, String targetType, List excludes,
                                           List outEntries, List outStyleableKeys) {
        if (!file.exists()) return

        file.eachLine { str ->
            if (str == '') return

            def i = str.indexOf(' ')
            str = str.substring(i + 1)
            i = str.indexOf(' ')
            def type = str.substring(0, i)
            if (targetType != null && type != targetType) return

            str = str.substring(i + 1)
            i = str.indexOf(' ')
            def name = str.substring(0, i)
            if (excludes != null && excludes.contains(name)) return

            if (type == 'styleable') {
                if (outStyleableKeys != null) {
                    outStyleableKeys.add(name)
                }
            } else {
                if (outEntries != null) {
                    outEntries.add(new Entry(type, name))
                }
            }
        }
    }

    public static void collectAarResourceKeys(File file, Set outEntries, Set outStyleableKeys) {
        if (!file.exists()) return

        file.eachLine { str ->
            if (str == '') return

            def i = str.indexOf(' ')
            str = str.substring(i + 1)
            i = str.indexOf(' ')
            def type = str.substring(0, i)

            str = str.substring(i + 1)
            i = str.indexOf(' ')
            def name = str.substring(0, i)

            if (type == 'styleable') {
                if (outStyleableKeys != null) {
                    outStyleableKeys.add(name)
                }
            } else {
                if (outEntries != null) {
                    outEntries.add(new Entry(type, name))
                }
            }
        }
    }
}
