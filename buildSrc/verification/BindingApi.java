// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

class BindingApi {
    private static final Set<String> LAYOUTS = Set.of(
        "size_t", "C_BOOL", "C_CHAR", "C_SHORT", "C_INT", "C_LONG_LONG", "C_FLOAT", "C_DOUBLE", "C_POINTER"
    );

    static void main(String[] args) throws Exception {
        var baseline = api(Path.of(args[0]));
        if (args.length == 1) {
            baseline.forEach(System.out::println);
            return;
        }
        baseline.removeAll(api(Path.of(args[1])));
        if (!baseline.isEmpty()) {
            throw new AssertionError("Missing or changed binding API:\n" + String.join("\n", baseline));
        }
    }

    private static Set<String> api(Path jar) throws Exception {
        var result = new TreeSet<String>();
        try (var loader = new URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, null)) {
            for (var typeName : new String[] {"PowerGridModelC", "PGM_LogOutputCallback", "PGM_LogOutputCallback$Function"}) {
                var type = Class.forName("org.lfenergy.pgm." + typeName, false, loader);
                for (var method : type.getMethods()) {
                    if (method.getDeclaringClass() == Object.class) {
                        continue;
                    }
                    if (typeName.equals("PowerGridModelC") && (!method.getName().startsWith("PGM_")
                            || method.getReturnType() == void.class && method.getParameterCount() == 1
                               && method.getName().startsWith("PGM_def_"))) {
                        continue;
                    }
                    result.add(typeName + " " + Modifier.toString(method.getModifiers()) + " "
                        + method.getReturnType().getName() + " " + method.getName()
                        + java.lang.invoke.MethodType.methodType(void.class, method.getParameterTypes())
                            .descriptorString());
                }
                for (var field : type.getFields()) {
                    if (field.getName().startsWith("PGM_") || LAYOUTS.contains(field.getName())) {
                        result.add(typeName + " " + Modifier.toString(field.getModifiers()) + " "
                            + field.getType().getName() + " " + field.getName());
                    }
                }
            }
        }
        return result;
    }
}
