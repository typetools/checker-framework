package flowexpression;

import java.util.Collection;
import testlib.flowexpression.qual.FlowExp;

public class UsePrivate {
    void test(Private app_ppts, Private test_ppts) {

        Collection<@FlowExp("app_ppts.nameToPpt") String> app_ppt_names = app_ppts.nameStringSet();
        Collection<@FlowExp("test_ppts.nameToPpt") String> test_ppt_names =
                test_ppts.nameStringSet();
    }
}
