package com.didi.virtualapk.collector.res

/**
 * Represent styleable item in R symbol file
 * e.g.
 * int[] styleable TagLayout { 0x010100af, 0x7f0102b5, 0x7f0102b6 }
 * int styleable TagLayout_android_gravity 0
 *
 *
 * Styleable will not be recorded to the arsc file, and the representation
 * in the R file is different from other resource types, so separate representation
 *
 * @author zhengtao
 */
class StyleableEntry {

    /**
     * Name of a styleable entry, e.g. TagLayout or TagLayout_android_gravity
     */
    String name
    /**
     * Value of a styleable entry represent in R file, e.g. { 0x010100af, 0x7f0102b5, 0x7f0102b6 } or 0
     */
    String value

    /**
     * Type of a styleable entry value , int or int[]
     */
    String valueType

    public StyleableEntry(name, value, valueType) {
        this.name = name
        this.value = value
        this.valueType = valueType
    }

    /**
     * e.g.
     * when value of the entry is: { 0x010100af, 0x7f0102b5, 0x7f0102b6 }
     * this method return [0x010100af, 0x7f0102b5, 0x7f0102b6]
     *
     * @return value of styleable entry as list
     */
    def getValueAsList() {
        value.trim()[1..-2].split(',')*.trim()
    }

    /**
     * Generate the value of int[] type styleable entry by list of value element
     * @param values list of
     */
    def setValue(List<String> values) {
        value = "{ ${values.join(', ')} }"
    }

    def setValue(String value) {
        this.value = value
    }


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        StyleableEntry that = (StyleableEntry) o

        if (name != that.name) return false
        if (valueType != that.valueType) return false

        return true
    }

    int hashCode() {
        int result
        result = name.hashCode()
        result = 31 * result + valueType.hashCode()
        return result
    }
}