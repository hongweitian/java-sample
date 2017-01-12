package ph.annotations.metadata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
// �����������ע�⽫Ӧ����ʲô�ط�,����Ӧ��Ϊ����
@Retention(RetentionPolicy.SOURCE)
// ���������ע������һ���������,��Դ������(source)���ļ���(class)��������ʱ(runtime)
public @interface GenTestObj {
	String value();
}
