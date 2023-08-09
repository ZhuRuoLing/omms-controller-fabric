package net.zhuruoling.omms.controller.fabric.permission;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionRuleFile {
    File file;
    String namespace;
    String name;
    List<PermissionRule> permissionRules;

    public PermissionRuleFile(File file, String namespace, String name, List<PermissionRule> permissionRules) {
        this.file = file;
        this.namespace = namespace;
        this.name = name;
        this.permissionRules = permissionRules;
    }

    public static PermissionRuleFile readFromFile(File file) {
        try {
            var fr = new FileReader(file);
            var reader = new BufferedReader(fr);
            List<String> lines = new ArrayList<>();
            reader.lines().forEach(lines::add);
            reader.close();
            var meta = lines.get(0).split(" ");
            String namespace = meta[1];
            String n = meta[0];
            List<PermissionRule> p = new ArrayList<>();
            List<String> ruleLines = new ArrayList<>();
            lines.subList(1, lines.size()).forEach(s -> {
                var l = Arrays.stream(s.split("\\|")).toList();
                var s1 = l.get(0).replace(" ", "");
                l.subList(1, l.size())
                        .stream().filter(rr -> !rr.isEmpty())
                        .forEach(s2 -> ruleLines.add((s1 + s2).stripLeading().stripTrailing()));
            });
            ruleLines.forEach(s -> p.add(PermissionRule.fromString(namespace, s)));
            return new PermissionRuleFile(file, namespace, n, p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
