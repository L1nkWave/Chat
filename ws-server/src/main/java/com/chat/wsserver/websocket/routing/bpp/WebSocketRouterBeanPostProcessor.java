package com.chat.wsserver.websocket.routing.bpp;

import com.chat.wsserver.websocket.routing.broadcast.BroadcastManager;
import com.chat.wsserver.websocket.routing.RouteComponent;
import com.chat.wsserver.websocket.routing.WebSocketRouter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.Map.Entry;

import static java.lang.String.format;

@Slf4j
@Component
public class WebSocketRouterBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean,
                                                 @NonNull String beanName) throws BeansException {
        Class<?> cls = bean.getClass();
        if (!Arrays.asList(cls.getInterfaces()).contains(WebSocketRouter.class)) {
            return bean;
        }

        Field routesField = getRoutesMapField(cls);
        ReflectionUtils.makeAccessible(routesField);

        var sb = new StringBuilder();
        Map<String, Object> routeBeans = applicationContext.getBeansWithAnnotation(WebSocketRoute.class);
        Map<String, RouteComponent> routes = new HashMap<>();

        // scan all routes
        for (Entry<String, Object> entry : routeBeans.entrySet()) {
            Class<?> routeCls = entry.getValue().getClass();
            String rootPath = routeCls.getAnnotation(WebSocketRoute.class).value();
            sb.append(rootPath);

            // scan all sub-routes
            for (Method method : routeCls.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SubRoute.class)) {
                    SubRoute ann = method.getAnnotation(SubRoute.class);

                    // omit disabled route handler
                    if (ann.disabled()) {
                        continue;
                    }

                    String path = ann.value();
                    sb.append(path);

                    String combinedPath = sb.toString();
                    if (routes.containsKey(combinedPath)) {
                        throw new RuntimeException("found duplicate routes");
                    }

                    // check broadcast options
                    verifyBroadcast(method);

                    method.setAccessible(true);
                    routes.put(combinedPath, new RouteComponent(entry.getValue(), method));

                    // restore sb
                    sb.setLength(0);
                    sb.append(rootPath);

                    log.debug("Route [{}], broadcast: {}", combinedPath, method.isAnnotationPresent(Broadcast.class));
                }
            }
            sb.setLength(0);
        }

        ReflectionUtils.setField(routesField, bean, Collections.unmodifiableMap(routes));
        return bean;
    }

    private void verifyBroadcast(@NonNull Method routeHandler) {
        if (!routeHandler.isAnnotationPresent(Broadcast.class)) {
            return;
        }

        if (routeHandler.getReturnType().equals(void.class)) {
            throw new RuntimeException(
                    format("Route handler \"%s\" with broadcast has return type void", routeHandler.getName())
            );
        }

        String[] keyComponents = routeHandler.getAnnotation(Broadcast.class)
                .value()
                .trim()
                .split(BroadcastManager.KEY_SEPARATOR);

        if (keyComponents.length < 2) {
            String errMsg = format(
                    "Broadcast annotation value incorrect format at route handler \"%s\"",
                    routeHandler.getName()
            );
            throw new RuntimeException(errMsg);
        }
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
