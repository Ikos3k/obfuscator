package pl.alpheratzteam.obfuscator.core.transformer;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;
import pl.alpheratzteam.obfuscator.api.transformer.Transformer;
import pl.alpheratzteam.obfuscator.api.util.AccessUtil;

import java.util.*;

/**
 * @author Unix on 03.09.2019.
 */
public class MethodCallTransformer implements Transformer {
    @Override
    public void transform(@NotNull Map<String, ClassNode> classMap) {
        final Map<String, Set<MethodNode>> methods = new HashMap<>();

        final Optional<ClassNode> mainClass = classMap.values()
                .stream()
                .filter(classNode -> classNode.methods
                        .stream()
                        .anyMatch(methodNode -> methodNode.name.equals("main") || methodNode.name.equals("premain")))
                .findFirst();

        mainClass.ifPresent(clazz -> {
            classMap.values().forEach(classNode -> {
                if (mainClass.get().name.equals(classNode.name)) {
                    return;
                }

                final Set<MethodNode> methodSet = new HashSet<>();

                classNode.methods
                        .stream()
                        .filter(methodNode -> AccessUtil.isStatic(methodNode.access))
                        .forEach(methodSet::add);

                methods.put(classNode.name, methodSet);
            });

            methods.forEach((key, value) -> {
                if (key.equals("<init>")) {
                    return;
                }

                clazz.methods.addAll(value);

                classMap.values().stream()
                        .filter(classNode -> classNode.name.equals(key))
                        .findFirst()
                        .ifPresent(aClass -> aClass.methods.removeAll(value));
            });

            classMap.values().forEach(classNode -> classNode.methods.forEach(methodNode -> {
                if (!classNode.name.equals(clazz.name)) {
                    return;
                }

                if (methodNode.name.equals("<init>")) {
                    return;
                }

                Arrays.stream(methodNode.instructions.toArray()).forEachOrdered(ain -> {
                            if (!(ain instanceof MethodInsnNode)) {
                                return;
                            }

                            final MethodInsnNode methodInsnNode = (MethodInsnNode) ain;

                            if (!this.isLibrary(classNode.methods, methodInsnNode.name)) {
                                return;
                            }

                            methodNode.instructions.insertBefore(ain, new MethodInsnNode(INVOKESTATIC, clazz.name, methodInsnNode.name, methodInsnNode.desc, false));
                            methodNode.instructions.remove(ain);
                        });
            }));
        });
    }

    @Override
    public String getName() {
        return "MethodCall";
    }

    private boolean isLibrary(@NotNull List<MethodNode> methodNodes, String methodName) {
        return methodNodes.stream().anyMatch(mn -> mn.name.equals(methodName));
    }
}