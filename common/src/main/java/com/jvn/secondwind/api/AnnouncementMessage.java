package com.jvn.secondwind.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.GsonHelper;

/** A text or translatable announcement. Plain text is fully supplied by server data. */
public record AnnouncementMessage(String translationKey, String fallback, String text) {
    public AnnouncementMessage {
        boolean translated = translationKey != null && !translationKey.isBlank();
        boolean plainText = text != null;
        if (translated == plainText) {
            throw new IllegalArgumentException("announcement requires exactly one of translate or text");
        }
        if (!translated && fallback != null) throw new IllegalArgumentException("text announcements cannot have a fallback");
    }

    public AnnouncementMessage(String translationKey, String fallback) {
        this(translationKey, fallback, null);
    }

    public static AnnouncementMessage text(String text) {
        return new AnnouncementMessage(null, null, text);
    }

    public static AnnouncementMessage parse(JsonElement element, String name) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return new AnnouncementMessage(element.getAsString(), null);
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException(name + " must be a translation key or an object");
        }

        JsonObject object = element.getAsJsonObject();
        if (object.has("text")) {
            if (object.has("translate") || object.has("fallback")) {
                throw new IllegalArgumentException(name + " cannot combine text with translate or fallback");
            }
            return text(GsonHelper.getAsString(object, "text"));
        }
        return new AnnouncementMessage(
                GsonHelper.getAsString(object, "translate"),
                object.has("fallback") ? GsonHelper.getAsString(object, "fallback") : null);
    }

    public MutableComponent render(Component subject, boolean localize) {
        if (text != null) return renderText(subject);
        MutableComponent translated = Component.translatableWithFallback(translationKey, fallback, subject);
        return localize ? translated : Component.literal(translated.getString());
    }

    private MutableComponent renderText(Component subject) {
        MutableComponent rendered = Component.empty();
        int start = 0;
        int placeholder;
        while ((placeholder = text.indexOf("%1$s", start)) >= 0) {
            if (placeholder > start) rendered.append(text.substring(start, placeholder));
            rendered.append(subject.copy());
            start = placeholder + 4;
        }
        if (start < text.length()) rendered.append(text.substring(start));
        return rendered;
    }
}
