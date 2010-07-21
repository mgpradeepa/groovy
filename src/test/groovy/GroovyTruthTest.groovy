package groovy

class GroovyTruthTest extends GroovyTestCase {

    void testTruth() {
        testFalse null

        assertTrue Boolean.TRUE
        testTrue true
        testFalse Boolean.FALSE
        testFalse false

        testFalse ""
        testTrue "bla"
        testTrue "true"
        testTrue "TRUE"
        testTrue "false"
        testFalse ''
        testTrue 'bla'
        testTrue new StringBuffer('bla')
        testFalse new StringBuffer()

        testFalse Collections.EMPTY_LIST
        testFalse([])
        testTrue([1])
        testFalse([].toArray())

        testFalse [:]
        testTrue([bla: 'some value'])
        testTrue 1234
        testFalse 0
        testTrue 0.3f
        testTrue new Double(3.0f)
        testFalse 0.0f
        testTrue new Character((char) 1)
        testFalse new Character((char) 0)
    }

    void testPrimitiveArrayTruth() {
        testTrue([1] as byte[])
        testTrue([1] as short[])
        testTrue([1] as int[])
        testTrue([1] as long[])
        testTrue([1] as float[])
        testTrue([1] as double[])
        testTrue([true] as boolean[])
        testTrue([1] as char[])

        testFalse([] as byte[])
        testFalse([] as short[])
        testFalse([] as int[])
        testFalse([] as long[])
        testFalse([] as float[])
        testFalse([] as double[])
        testFalse([] as boolean[])
        testFalse([] as char[])
    }

    void testIteratorTruth() {
        testFalse([].iterator())
        testTrue([1].iterator())
    }

    void testEnumerationTruth() {
        def v = new Vector()
        testFalse(v.elements())
        v.add(new Object())
        testTrue(v.elements())
    }

    protected testTrue(someObj) {
        assertTrue someObj ? true : false
    }

    protected testFalse(someObj) {
        assertFalse someObj ? true : false
    }

}