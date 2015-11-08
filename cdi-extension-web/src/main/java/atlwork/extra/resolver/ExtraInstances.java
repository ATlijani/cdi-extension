package atlwork.extra.resolver;

import javax.enterprise.context.ApplicationScoped;

import atlwork.cdi.extension.extra.ExtraResolver;

@ApplicationScoped
public class ExtraInstances {

    @ExtraResolver
    public Object resolveExtra(String key) {

	if (key.equals("person")) {
	    return new Person();
	} else if (key.equals("car")) {
	    return new Car();
	}

	return null;
    }

    public static class Person {

	@Override
	public String toString() {
	    return "Person []";
	}
    }

    public static class Car {

	@Override
	public String toString() {
	    return "Car []";
	}
    }

}
