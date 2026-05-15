package com.jvn.secondwind.client.config;

import com.jvn.secondwind.config.CooldownMode;
import com.jvn.secondwind.config.SecondWindConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SecondWindConfigScreen extends Screen {
    private static final int ENTRIES_PER_PAGE = 8;
    private static final int LABEL_X = 32;
    private static final int CONTROL_WIDTH = 150;
    private static final List<Entry> ENTRIES = List.of(
            new IntEntry("downedTimerSeconds", SecondWindConfig.DOWNED_TIMER_SECONDS),
            new IntEntry("minimumDownedTimerSeconds", SecondWindConfig.MINIMUM_DOWNED_TIMER_SECONDS),
            new IntEntry("timerPenaltyPerDown", SecondWindConfig.TIMER_PENALTY_PER_DOWN),
            new IntEntry("downedSlownessLevel", SecondWindConfig.DOWNED_SLOWNESS_LEVEL),
            new BooleanEntry("downedDamageReducesTimer", SecondWindConfig.DOWNED_DAMAGE_REDUCES_TIMER),
            new IntEntry("downedDamageCooldownTicks", SecondWindConfig.DOWNED_DAMAGE_COOLDOWN_TICKS),
            new BooleanEntry("downedDamageRegisters", SecondWindConfig.DOWNED_DAMAGE_REGISTERS),
            new BooleanEntry("blockHealingWhileDowned", SecondWindConfig.BLOCK_HEALING_WHILE_DOWNED),
            new BooleanEntry("blockEatingWhileDowned", SecondWindConfig.BLOCK_EATING_WHILE_DOWNED),
            new IntEntry("reviveHealthHalfHearts", SecondWindConfig.REVIVE_HEALTH_HALF_HEARTS),
            new IntEntry("reviveRegenerationSeconds", SecondWindConfig.REVIVE_REGENERATION_SECONDS),
            new IntEntry("postReviveInvulnerabilitySeconds", SecondWindConfig.POST_REVIVE_INVULNERABILITY_SECONDS),
            new CooldownEntry("cooldownMode", SecondWindConfig.COOLDOWN_MODE),
            new IntEntry("cooldownDurationSeconds", SecondWindConfig.COOLDOWN_DURATION_SECONDS),
            new BooleanEntry("resetCooldownOnDeath", SecondWindConfig.RESET_COOLDOWN_ON_DEATH),
            new BooleanEntry("multiplayerRevive", SecondWindConfig.MULTIPLAYER_REVIVE),
            new DoubleEntry("reviveChannelSeconds", SecondWindConfig.REVIVE_CHANNEL_SECONDS),
            new DoubleEntry("reviveDistance", SecondWindConfig.REVIVE_DISTANCE),
            new BooleanEntry("reviveInterruptOnDamage", SecondWindConfig.REVIVE_INTERRUPT_ON_DAMAGE),
            new BooleanEntry("allowPassiveKills", SecondWindConfig.ALLOW_PASSIVE_KILLS),
            new BooleanEntry("allowPlayerKills", SecondWindConfig.ALLOW_PLAYER_KILLS),
            new BooleanEntry("allowPetKills", SecondWindConfig.ALLOW_PET_KILLS),
            new BooleanEntry("allowVoidSecondWind", SecondWindConfig.ALLOW_VOID_SECOND_WIND),
            new BooleanEntry("enableDownedVignette", SecondWindConfig.ENABLE_DOWNED_VIGNETTE),
            new BooleanEntry("enableDesaturation", SecondWindConfig.ENABLE_DESATURATION),
            new BooleanEntry("enableDownedBloom", SecondWindConfig.ENABLE_DOWNED_BLOOM),
            new BooleanEntry("enableSounds", SecondWindConfig.ENABLE_SOUNDS));

    private final Screen parent;
    private int page;

    public SecondWindConfigScreen(Screen parent) {
        super(Component.translatable("secondwind.configuration.title", Component.literal("Second Wind!")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int controlX = width - LABEL_X - CONTROL_WIDTH;
        int y = 48;
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, ENTRIES.size());
        for (int index = start; index < end; index++) {
            addRenderableWidget(ENTRIES.get(index).create(this, controlX, y));
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildWidgets();
                })
                .bounds(width / 2 - 94, height - 54, 28, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    page = Math.min(maxPage(), page + 1);
                    rebuildWidgets();
                })
                .bounds(width / 2 + 66, height - 54, 28, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> {
                    SecondWindConfig.resetDefaults();
                    rebuildWidgets();
                })
                .bounds(width / 2 - 154, height - 28, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
                    SecondWindConfig.save();
                    minecraft.setScreen(parent);
                })
                .bounds(width / 2 - 48, height - 28, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> closeWithoutSaving())
                .bounds(width / 2 + 58, height - 28, 96, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Page " + (page + 1) + " / " + (maxPage() + 1), width / 2, height - 49, 0xFFA0A0A0);

        int y = 54;
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, ENTRIES.size());
        for (int index = start; index < end; index++) {
            graphics.drawString(font, ENTRIES.get(index).label(), LABEL_X, y, 0xFFFFFFFF);
            y += 24;
        }
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    private void closeWithoutSaving() {
        SecondWindConfig.load();
        minecraft.setScreen(parent);
    }

    private int maxPage() {
        return Math.max(0, (ENTRIES.size() - 1) / ENTRIES_PER_PAGE);
    }

    private interface Entry {
        Component label();

        net.minecraft.client.gui.components.AbstractWidget create(Screen screen, int x, int y);
    }

    private record IntEntry(String key, SecondWindConfig.IntValue value) implements Entry {
        @Override
        public Component label() {
            return Component.translatable("secondwind.configuration." + key);
        }

        @Override
        public EditBox create(Screen screen, int x, int y) {
            EditBox box = new EditBox(Minecraft.getInstance().font, x, y, CONTROL_WIDTH, 20, label());
            box.setValue(value.get().toString());
            box.setResponder(text -> {
                try {
                    value.set(Integer.parseInt(text.trim()));
                } catch (NumberFormatException ignored) {
                }
            });
            return box;
        }
    }

    private record DoubleEntry(String key, SecondWindConfig.DoubleValue value) implements Entry {
        @Override
        public Component label() {
            return Component.translatable("secondwind.configuration." + key);
        }

        @Override
        public EditBox create(Screen screen, int x, int y) {
            EditBox box = new EditBox(Minecraft.getInstance().font, x, y, CONTROL_WIDTH, 20, label());
            box.setValue(value.get().toString());
            box.setResponder(text -> {
                try {
                    value.set(Double.parseDouble(text.trim()));
                } catch (NumberFormatException ignored) {
                }
            });
            return box;
        }
    }

    private record BooleanEntry(String key, SecondWindConfig.BooleanValue value) implements Entry {
        @Override
        public Component label() {
            return Component.translatable("secondwind.configuration." + key);
        }

        @Override
        public Button create(Screen screen, int x, int y) {
            return Button.builder(text(), button -> {
                        value.toggle();
                        button.setMessage(text());
                    })
                    .bounds(x, y, CONTROL_WIDTH, 20)
                    .build();
        }

        private Component text() {
            return value.get() ? Component.translatable("options.on") : Component.translatable("options.off");
        }
    }

    private record CooldownEntry(String key, SecondWindConfig.EnumValue<CooldownMode> value) implements Entry {
        @Override
        public Component label() {
            return Component.translatable("secondwind.configuration." + key);
        }

        @Override
        public Button create(Screen screen, int x, int y) {
            return Button.builder(text(), button -> {
                        value.next();
                        button.setMessage(text());
                    })
                    .bounds(x, y, CONTROL_WIDTH, 20)
                    .build();
        }

        private Component text() {
            return value.get().getTranslatedName();
        }
    }
}
