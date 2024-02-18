import com.esri.core.geometry.TestSingleton;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses( { AllTests.class, })
public class _BeforeAll {

    @BeforeClass
    public static void a_beforeClass() {
        System.out.println("====BEFORE====");
        TestSingleton.getInstance().init();
    }


}