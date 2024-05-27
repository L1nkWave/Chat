package org.linkwave.ws.websocket.routing.bpp;

import lombok.extern.slf4j.Slf4j;
import org.linkwave.ws.websocket.routing.EndpointCondition;
import org.linkwave.ws.websocket.routing.RouteComponent;
import org.linkwave.ws.websocket.routing.WebSocketRouter;
import org.linkwave.ws.websocket.routing.broadcast.BroadcastManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableMap;

@Slf4j
@Component
public class WebSocketRouterBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean,
                                                 @NonNull String beanName) throws BeansException {
        if (!Arrays.asList(bean.getClass().getInterfaces()).contains(WebSocketRouter.class)) {
            return bean;
        }

        final Field routesField = getRoutesMapField(bean.getClass());
        ReflectionUtils.makeAccessible(routesField);

        final long start = currentTimeMillis();

        final Map<String, Object> routeBeans = applicationContext.getBeansWithAnnotation(WebSocketRoute.class);
        final Map<String, RouteComponent> routes = scanRoutes(routeBeans);
        ReflectionUtils.setField(routesField, bean, unmodifiableMap(routes));

        final long end = currentTimeMillis();

        log.info("Build {} took {} ms", bean.getClass().getSimpleName(), end - start);

        return bean;
    }

    @NonNull
    private Map<String, RouteComponent> scanRoutes(@NonNull Map<String, Object> routeBeans) {
        final Map<String, RouteComponent> routes = new HashMap<>();
        var sb = new StringBuilder();

        for (Entry<String, Object> entry : routeBeans.entrySet()) {
            Class<?> routeCls = entry.getValue().getClass();
            String rootPath = routeCls.getAnnotation(WebSocketRoute.class).value();
            sb.append(rootPath);

            // scan all endpoints inside bean
            for (Method method : routeCls.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Endpoint.class)) {
                    Endpoint endpoint = method.getAnnotation(Endpoint.class);

                    // omit disabled endpoint
                    if (endpoint.disabled()) {
                        continue;
                    }

                    String path = endpoint.value();
                    sb.append(path);

                    String combinedPath = sb.toString();
                    if (routes.containsKey(combinedPath)) {
                        throw new RuntimeException("Found duplicate routes");
                    }

                    // check broadcast options
                    final boolean broadcast = verifyBroadcast(method);

                    // find conditions
                    final List<EndpointCondition> conditions = Arrays.stream(endpoint.conditions())
                            .map(applicationContext::getBean)
                            .map(condition -> (EndpointCondition) condition)
                            .toList();

                    method.setAccessible(true);
                    routes.put(combinedPath, new RouteComponent(entry.getValue(), method, conditions));

                    // restore sb
                    sb.setLength(0);
                    sb.append(rootPath);

                    log.debug("Route [{}], broadcast: {}, conditions: {}", combinedPath, broadcast, conditions.size());
                }
            }
            sb.setLength(0);
        }
        return routes;
    }

    private boolean verifyBroadcast(@NonNull Method routeHandler) {
        final Broadcast[] annotations = routeHandler.getAnnotationsByType(Broadcast.class);
        if (annotations.length == 0) {
            return false;
        }

        if (routeHandler.getReturnType().equals(void.class)) {
            throw new RuntimeException(
                    format(
                            "Route handler \"%s\" marked as broadcast has return type void",
                            "%s.%s".formatted(routeHandler.getDeclaringClass().getName(), routeHandler.getName())
                    )
            );
        }

        for (Broadcast annotation : annotations) {
            final String[] keyComponents = annotation.value()
                    .trim()
                    .split(BroadcastManager.KEY_SEPARATOR);

            if (keyComponents.length < 2) {
                String errMsg = format(
                        "Broadcast annotation value incorrect format at route handler \"%s\"",
                        "%s.%s".formatted(routeHandler.getDeclaringClass().getName(), routeHandler.getName())
                );
                throw new RuntimeException(errMsg);
            }
        }
        return true;
    }

    private Field getRoutesMapField(@NonNull Class<?> cls) {

        // find Map<String, RouteComponent> fields for every WebSocketRouter bean
        List<Field> mapFields = Arrays.stream(cls.getDeclaredFields())
                .filter(field -> field.getType().equals(Map.class))
                .filter(field -> {
                    var genericType = (ParameterizedType) field.getGenericType();
                    Type[] types = genericType.getActualTypeArguments();
                    return types[0].equals(String.class) && types[1].equals(RouteComponent.class);
                })
                .toList();

        // validate map fields
        if (mapFields.isEmpty()) {
            throw new BeanInitializationException(
                    format("No routes map field declared in [%s]", cls.getName())
            );
        } else if (mapFields.size() > 1) {
            throw new BeanInitializationException(
                    format("Bean [%s] must contain only one routes map field", cls.getName())
            );
        }

        return mapFields.get(0);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
