package com.fcproject.adapters.inbound.mappers;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;

import java.util.ArrayList;
import java.util.List;

public class ObjectMapper {

    //Dozer mapper
    private static Mapper mapper = DozerBeanMapperBuilder.buildDefault();

    //Parse for dto to entity or entity to dto
    public static <O, D> D parseObject(O origin, Class<D> destination) {

        return mapper.map(origin, destination);
    }

    //Parse for lists
    public static <O, D> List<D> parseListObjects(List<O> origin, Class<D> destination) {
        List<D> destinationObjects = new ArrayList<D>();
        for(Object o : origin) {
            destinationObjects.add(mapper.map(o, destination));
        }

        return destinationObjects;
    }
}
