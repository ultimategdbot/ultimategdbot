package com.github.alex1304.ultimategdbot.api.command;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.github.alex1304.ultimategdbot.api.command.argument.ArgumentParser;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface CommandAction {
	Class<? extends ArgumentParser<?>>[] value() default {};
}
