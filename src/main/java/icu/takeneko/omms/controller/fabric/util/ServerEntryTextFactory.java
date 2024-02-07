package icu.takeneko.omms.controller.fabric.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ServerEntryTextFactory {
    // /tellraw @a [{"text":"[","color":"white"},{"text":"%s","color":"aqua","clickEvent":{"action":"run_command","value":"/server %S"},"hoverEvent":{"action":"show_text","contents":{"text":"进入%s","color":"white"}}},{"text":"]","color":"white"}]
    public static Collection<Text> generateServerEntryText(String displayName, String proxyName, boolean isCurrent) {
        HashSet<Text> texts = new HashSet<>(3);
        Text left = Texts.toText(() -> "[");
        String color = "aqua";
        if (isCurrent)
            color = "yellow";
        String finalColor = color;
        Gson gson = new GsonBuilder().create();
        var jsonElement = gson.toJsonTree("{\"text\":\"%s\",\"color\":\"%s\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/server %S\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":{\"text\":\"进入%s\",\"color\":\"white\"}}}"
                .formatted(displayName, color, proxyName, displayName)
        );
        List<Text> serverText = Texts.toText(() -> displayName).getWithStyle(new Style.Serializer().deserialize(jsonElement, Style.class, null));
        Text right = Texts.toText(() -> "]");
        texts.add(left);
        texts.addAll(serverText);
        texts.add(right);
        return texts;
    }

}
