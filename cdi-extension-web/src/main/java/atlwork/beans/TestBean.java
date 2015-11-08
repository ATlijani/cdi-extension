package atlwork.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import atlwork.cdi.extension.extra.Extra;
import atlwork.extra.resolver.ExtraInstances.Car;
import atlwork.extra.resolver.ExtraInstances.Person;

@Named
@RequestScoped
public class TestBean {

    private String name;

    @Extra("person")
    private Person person;

    @Extra(value = "car")
    private Car car;

    public Person getPerson() {
	return person;
    }

    public Car getCar() {
	return car;
    }

}
