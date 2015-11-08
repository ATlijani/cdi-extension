package atlwork.cdi.extension.extra;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

public class ExtraExtension<M> implements Extension {

    private ExtraResolverInvoker extraResolverInvoker;

    void processAnnotatedType(@Observes ProcessAnnotatedType<M> pat, BeanManager beanManager) {

	AnnotatedType<M> scannedType = pat.getAnnotatedType();

	for (AnnotatedMethod<? super M> method : scannedType.getMethods()) {
	    if (method.isAnnotationPresent(ExtraResolver.class)) {
		extraResolverInvoker = new ExtraResolverInvoker(beanManager, method);
		break;
	    }
	}
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation adv) {
	extraResolverInvoker.initialize();
    }

    <T> void processInjectionTarget(@Observes ProcessInjectionTarget<T> processInjectionTarget) {

	final InjectionTarget<T> injectionTarget = processInjectionTarget.getInjectionTarget();
	final AnnotatedType<T> annotatedType = processInjectionTarget.getAnnotatedType();

	boolean hasExtra = false;
	for (Field field : annotatedType.getJavaClass().getDeclaredFields()) {
	    if (field.getAnnotation(Extra.class) != null) {
		hasExtra = true;
	    }
	}
	if (!hasExtra) {
	    return;
	}
	InjectionTarget<T> wrapper = new InjectionTarget<T>() {

	    @Override
	    public T produce(CreationalContext<T> ctx) {
		return injectionTarget.produce(ctx);
	    }

	    @Override
	    public void dispose(T instance) {
		injectionTarget.dispose(instance);
	    }

	    @Override
	    public Set<InjectionPoint> getInjectionPoints() {
		return injectionTarget.getInjectionPoints();
	    }

	    @Override
	    public void inject(T instance, CreationalContext<T> ctx) {
		injectionTarget.inject(instance, ctx);
		for (Field field : annotatedType.getJavaClass().getDeclaredFields()) {
		    if (field.getAnnotation(Extra.class) != null) {
			try {
			    String key = field.getAnnotation(Extra.class).value();
			    field.setAccessible(true);
			    field.set(instance, extraResolverInvoker.resolve(key, ctx));
			} catch (Exception e) {
			    throw new RuntimeException("Could not resolve instance", e);
			}
		    }
		}
	    }

	    @Override
	    public void postConstruct(T instance) {
		injectionTarget.postConstruct(instance);
	    }

	    @Override
	    public void preDestroy(T instance) {
		injectionTarget.preDestroy(instance);
	    }

	};

	processInjectionTarget.setInjectionTarget(wrapper);
    }

    public class ExtraResolverInvoker {

	private final BeanManager beanManager;
	private final AnnotatedMethod<? super M> resolverMethod;
	private Object extraResolverBean;

	private ExtraResolverInvoker(BeanManager beanManager, AnnotatedMethod<? super M> resolverMethod) {
	    this.beanManager = beanManager;
	    this.resolverMethod = resolverMethod;
	}

	private void initialize() {

	    Set<Bean<?>> beans = beanManager.getBeans(resolverMethod.getJavaMember().getDeclaringClass());
	    Bean<? extends Object> resolvedBeans = beanManager.resolve(beans);
	    CreationalContext<?> creationalContext = beanManager.createCreationalContext(resolvedBeans);

	    extraResolverBean = beanManager.getReference(resolvedBeans, resolverMethod.getJavaMember().getDeclaringClass(), creationalContext);
	}

	public Object resolve(String key, CreationalContext<?> ctx) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

	    return resolverMethod.getJavaMember().invoke(extraResolverBean, new Object[] { key });
	}
    }

}
