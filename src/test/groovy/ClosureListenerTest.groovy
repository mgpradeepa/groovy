package groovy

import javax.swing.JButton

/**
 * @version $Revision$
 */
class ClosureListenerTest extends GroovyTestCase {

    private static boolean headless

    static {
        try {
            Class.forName "javax.swing.JButton"
            headless = false
        } catch (java.awt.HeadlessException e) {
            headless = true
        } catch (java.lang.UnsatisfiedLinkError e) {
            headless = true
        }
    }

    void testAddingAndRemovingAClosureListener() {
        if (headless) return

        def b = new JButton("foo")
        b.actionPerformed = { println("Found ${it}") }

        def size = b.actionListeners.size()
        assert size == 1
        
        def l = b.actionListeners[0]
		def code = l.hashCode()
        
        println("listener: ${l} with hashCode code ${code}")
        
        assert l.toString() != "null"
        
        assert !l.equals(b)
        assert l.equals(l)
        
        assert l.hashCode() != 0
        
        b.removeActionListener(l)
        
        println(b.actionListeners)
        
        size = b.actionListeners.size()
        assert size == 0
    }
    
    void testGettingAListenerProperty() {
        if (headless) return

    	def b = new JButton("foo")
    	def foo = b.actionPerformed
    	assert foo == null
    }
    
    void testNonStandardListener() {
        def myWhat = null
        def myWhere = null

        def strangeBean = new StrangeBean()
        strangeBean.somethingStrangeHappened = { what, where -> myWhat = what; myWhere = where}
        strangeBean.somethingStrangeHappened('?', '!')
    
        assert myWhat == '?'
        assert myWhere == '!'
    }
}