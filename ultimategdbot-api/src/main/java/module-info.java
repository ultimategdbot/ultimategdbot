module ultimategdbot.api {
	requires transitive discord4j.core;
	requires reactor.core;
	requires org.reactivestreams;
	requires discord4j.rest;
	requires io.netty.codec.http;
	
	exports com.github.alex1304.ultimategdbot.api.entity;
	exports com.github.alex1304.ultimategdbot.api;
}