package net.zhuruoling.omms.controller.fabric.permission;

import com.mojang.logging.LogUtils;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

// com.ishland.earlyloadingscreen.patch.PatchUtil
// com.ishland.earlyloadingscreen.patch.FabricLoaderInvokePatch
public class PatchUtil {
    private static final Path transformerOutputPath = Path.of(".", ".omms-controller-fabric-transformer-output");
    private static final Logger logger = LogUtils.getLogger();
    static Instrumentation instrumentation;

    private static final CopyOnWriteArrayList<BiFunction<String, ClassNode, Boolean>> transformerLists = new CopyOnWriteArrayList<>();
    private static final BiFunction<String, ClassNode, Boolean> PERMISSION_TRANSFORMER = PatchUtil::patchMethod;

    public static void init() {
    }

    static {
        if (Files.isDirectory(transformerOutputPath)) {
            try {
                deleteDirectory();
            } catch (IOException e) {
                logger.warn("Failed to delete transformer output directory", e);
            }
        }
        try {
            Files.createDirectories(transformerOutputPath);
        } catch (IOException e) {
            logger.warn("Failed to create transformer output directory", e);
        }
        Instrumentation inst = null;
        try {
            inst = ByteBuddyAgent.install();
        } catch (Throwable t) {
            logger.error("Failed to install ByteBuddyAgent, patching will not work", t);
        }
        instrumentation = inst;

        if (inst != null) {
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    try {
                        if (transformerLists.isEmpty())return null;
                        ClassNode node = new ClassNode();
                        new ClassReader(classfileBuffer).accept(node, 0);
                        boolean transformed = false;
                        for (BiFunction<String, ClassNode, Boolean> fn : transformerLists) {
                            transformed |= fn.apply(className, node);
                        }
                        if (transformed) {
                            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                            node.accept(writer);
                            final byte[] buf = writer.toByteArray();
                            try {
                                final Path path = transformerOutputPath.resolve(className + ".class");
                                Files.createDirectories(path.getParent());
                                Files.write(path, buf);
                            } catch (Throwable t) {
                                logger.warn("Failed to write transformed class %s to disk".formatted(className), t);
                            }
                            return buf;
                        } else {
                            return null;
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to transform class " + className, t);
                        return null;
                    }
                }
            }, true);
        }
    }

    private static void updateAppLoaderAccess(Instrumentation inst) {
        inst.redefineModule(
                ModuleLayer.boot().findModule("java.base").get(),
                Set.of(),
                Map.of(),
                Map.of("java.lang", Set.of(PatchUtil.class.getModule())),
                Set.of(),
                Map.of()
        );
    }

    public static void initTransformer() {
        Instrumentation inst = PatchUtil.instrumentation;
        if (inst == null) {
            logger.warn("Instrumentation unavailable, entrypoint information will not be available");
            return;
        }
        try {
            updateAppLoaderAccess(inst);
        } catch (Throwable t) {
            logger.warn("Failed to update AppLoader access", t);
        }
    }

    public static void patchClass(String className) {
        transformerLists.add(PERMISSION_TRANSFORMER);
        try {
            var clazz = Class.forName(className);
            instrumentation.retransformClasses(clazz);
            transformerLists.remove(PERMISSION_TRANSFORMER);
        } catch (Throwable t) {
            logger.warn("Failed to transform %s, attempting to revert changes".formatted(className), t);
            transformerLists.remove(PERMISSION_TRANSFORMER);
            try {
                instrumentation.retransformClasses(Class.forName(className));
            } catch (Throwable t2) {
                logger.warn("Failed to revert changes to %s".formatted(className), t2);
            }
        }
    }

    private static void deleteDirectory() throws IOException {
        final Iterator<Path> iterator = Files.walk(PatchUtil.transformerOutputPath)
                .sorted(Comparator.reverseOrder()).iterator();
        while (iterator.hasNext()) {
            Files.delete(iterator.next());
        }
    }

    /*
    LDC ""
    ALOAD 1
    INVOKESTATIC net/zhuruoling/omms/controller/fabric/patch/PatchRuleManager.checkPermission (Ljava/lang/String;Lnet/minecraft/server/command/ServerCommandSource;)Z
    IRETURN
     */
    public static boolean patchMethod(String className, ClassNode node) {
        String clzName = MappedNames.nameOfClassServerCommandSource.replace('.','/');
        List<MethodNode> methodNodes = new ArrayList<>();
        for (MethodNode methodNode : node.methods) {
            if (!Objects.equals(methodNode.desc, "(L%s;)Z".formatted(clzName)))continue;
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                //target INVOKEVIRTUAL net/minecraft/server/command/ServerCommandSource.hasPermissionLevel (I)Z
                if (insnNode instanceof MethodInsnNode methodInsnNode) {
                    if (methodInsnNode.owner.equals(clzName) &&
                            methodInsnNode.name.equals(MappedNames.nameOfMethodHasPermissionLevel) &&
                            methodInsnNode.desc.equals("(I)Z")) {
                        methodNodes.add(methodNode);
                    }
                }
            }
        }
        if (methodNodes.isEmpty()) return false;
        boolean t = false;
        for (MethodNode methodNode : methodNodes) {
            var clazzName = className.replace("/",".");
            if (PermissionRuleManager.INSTANCE.containsClass(clazzName)) {
                methodNode.maxStack = 4;
                var iter = methodNode.instructions.iterator();
                iter.add(new LdcInsnNode(clazzName));
                iter.add(new VarInsnNode(Opcodes.ALOAD, 0));
                iter.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "net/zhuruoling/omms/controller/fabric/permission/PermissionRuleManager",
                        "checkPermission",
                        "(Ljava/lang/String;L%s;)Z".formatted(clzName)
                ));
                iter.add(new InsnNode(Opcodes.IRETURN));
                t = true;
            }
        }
        return t;
    }

}
