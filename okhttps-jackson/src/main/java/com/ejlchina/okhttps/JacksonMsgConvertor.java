package com.ejlchina.okhttps;

import com.ejlchina.okhttps.internal.HttpException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacksonMsgConvertor implements MsgConvertor, ConvertProvider {

	private ObjectMapper objectMapper;

	private final Map<Type, TypeReference<?>> cache = new HashMap<>();

	private boolean typeCached;

	public JacksonMsgConvertor() {
		this(new ObjectMapper());
	}
	
	public JacksonMsgConvertor(ObjectMapper objectMapper) {
		this(objectMapper, false);
	}

	public JacksonMsgConvertor(ObjectMapper objectMapper, boolean typeCached) {
		this.objectMapper = objectMapper;
		this.typeCached = typeCached;
	}

	@Override
	public String mediaType() {
		return "application/json";
	}

	@Override
	public Mapper toMapper(InputStream in, Charset charset) {
		try {
			JsonNode json = objectMapper.readTree(new InputStreamReader(in, charset));
			if (json.isObject()) {
				return new JacksonMapper((ObjectNode) json);
			}
			if (json.isNull() || json.isMissingNode()) {
				return null;
			}
			throw new HttpException("不是 一个 json 对象：" + json);
		} catch (IOException e) {
			throw new HttpException("Jackson 解析异常", e);
		}
	}

	@Override
	public Array toArray(InputStream in, Charset charset) {
		try {
			JsonNode json = objectMapper.readTree(new InputStreamReader(in, charset));
			if (json.isArray()) {
				return new JacksonArray((ArrayNode) json);
			}
			if (json.isNull() || json.isMissingNode()) {
				return null;
			}
			throw new HttpException("不是 一个 json 数组：" + json);
		} catch (IOException e) {
			throw new HttpException("Jackson 解析异常", e);
		}
	}

	@Override
	public byte[] serialize(Object object, Charset charset) {
		try {
			return objectMapper.writeValueAsString(object).getBytes(charset);
		} catch (JsonProcessingException e) {
			throw new HttpException("Java Bean [" + object + "] Jackson 序列化异常", e);
		}
	}

	private <T> TypeReference<T> toTypeRef(Type type) {
		TypeReference<T> typeRef;
		synchronized (cache) {
			//noinspection unchecked
			typeRef = (TypeReference<T>) cache.get(type);
			if (typeRef == null) {
				typeRef = new TypeReference<T>() {
					@Override
					public Type getType() {
						return type;
					}
				};
				if (typeCached) {
					cache.put(type, typeRef);
				}
			}
		}
		return typeRef;
	}

	@Override
	public <T> T toBean(Type type, InputStream in, Charset charset) {
		try {
			return objectMapper.readValue(new InputStreamReader(in, charset), toTypeRef(type));
		} catch (IOException e) {
			throw new HttpException("Jackson 解析异常", e);
		}
	}

	@Override
	public <T> List<T> toList(Class<T> type, InputStream in, Charset charset) {
		CollectionType javaType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, type);
		try {
			return objectMapper.readValue(new InputStreamReader(in, charset), javaType);
		} catch (IOException e) {
			throw new HttpException("Jackson 解析异常", e);
		}
	}

	@Override
	public MsgConvertor getConvertor() {
		return new JacksonMsgConvertor();
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public boolean isTypeCached() {
		return typeCached;
	}

	public void setTypeCached(boolean typeCached) {
		this.typeCached = typeCached;
	}

}
