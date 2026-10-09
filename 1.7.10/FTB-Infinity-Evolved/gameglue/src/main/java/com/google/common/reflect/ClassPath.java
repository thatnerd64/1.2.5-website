package com.google.common.reflect;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.net.URL;

/**
 * Guava 17's ClassPath, answered from the build's list of classes (retro/origins.txt) instead of scanning jar files
 * and folders, which do not exist in the browser. Mods use it to find their own classes in a package: Logistics
 * Pipes (packets and GUIs), Thaumic Tinkerer (blocks and items), JourneyMap (features), Headcrumbs (VIP list).
 * Only classes are listed as resources.
 */
public final class ClassPath {
    private final ImmutableSet<ResourceInfo> resources;

    private ClassPath(ImmutableSet<ResourceInfo> resources) {
        this.resources = resources;
    }

    public static ClassPath from(ClassLoader classloader) throws IOException {
        ImmutableSet.Builder<ResourceInfo> builder = ImmutableSet.builder();
        for (String name : new java.util.TreeSet<>(retro.rt.Origins.classNames())) {
            builder.add(new ClassInfo(name.replace('.', '/') + ".class", classloader));
        }
        return new ClassPath(builder.build());
    }

    public ImmutableSet<ResourceInfo> getResources() {
        return resources;
    }

    public ImmutableSet<ClassInfo> getAllClasses() {
        ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
        for (ResourceInfo resource : resources) {
            if (resource instanceof ClassInfo) {
                builder.add((ClassInfo) resource);
            }
        }
        return builder.build();
    }

    public ImmutableSet<ClassInfo> getTopLevelClasses() {
        ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
        for (ClassInfo info : getAllClasses()) {
            if (info.className.indexOf('$') == -1) {
                builder.add(info);
            }
        }
        return builder.build();
    }

    public ImmutableSet<ClassInfo> getTopLevelClasses(String packageName) {
        ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
        for (ClassInfo info : getTopLevelClasses()) {
            if (info.getPackageName().equals(packageName)) {
                builder.add(info);
            }
        }
        return builder.build();
    }

    public ImmutableSet<ClassInfo> getTopLevelClassesRecursive(String packageName) {
        String prefix = packageName + '.';
        ImmutableSet.Builder<ClassInfo> builder = ImmutableSet.builder();
        for (ClassInfo info : getTopLevelClasses()) {
            if (info.getName().startsWith(prefix)) {
                builder.add(info);
            }
        }
        return builder.build();
    }

    static String getClassName(String filename) {
        int classNameEnd = filename.length() - ".class".length();
        return filename.substring(0, classNameEnd).replace('/', '.');
    }

    public static class ResourceInfo {
        private final String resourceName;
        final ClassLoader loader;

        static ResourceInfo of(String resourceName, ClassLoader loader) {
            return resourceName.endsWith(".class") ? new ClassInfo(resourceName, loader)
                    : new ResourceInfo(resourceName, loader);
        }

        ResourceInfo(String resourceName, ClassLoader loader) {
            this.resourceName = resourceName;
            this.loader = loader;
        }

        public final URL url() {
            return loader.getResource(resourceName);
        }

        public final String getResourceName() {
            return resourceName;
        }

        @Override
        public int hashCode() {
            return resourceName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ResourceInfo) {
                ResourceInfo that = (ResourceInfo) obj;
                return resourceName.equals(that.resourceName) && loader == that.loader;
            }
            return false;
        }

        @Override
        public String toString() {
            return resourceName;
        }
    }

    public static final class ClassInfo extends ResourceInfo {
        private final String className;

        ClassInfo(String resourceName, ClassLoader loader) {
            super(resourceName, loader);
            this.className = getClassName(resourceName);
        }

        public String getPackageName() {
            int dot = className.lastIndexOf('.');
            return dot < 0 ? "" : className.substring(0, dot);
        }

        public String getSimpleName() {
            String name = className.substring(className.lastIndexOf('.') + 1);
            int dollar = name.lastIndexOf('$');
            return dollar < 0 ? name : name.substring(dollar + 1);
        }

        public String getName() {
            return className;
        }

        public Class<?> load() {
            try {
                return retro.compat.ReflectCompat.forName(className, false, loader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String toString() {
            return className;
        }
    }
}
