import com.esri.core.geometry.TestSingleton;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { AllTests.class, })
public class z_AfterAll {

    @AfterClass
    public static void z_afterClass() {
        System.out.println("====AFTER====");
        TestSingleton.getInstance().print();
    }

}