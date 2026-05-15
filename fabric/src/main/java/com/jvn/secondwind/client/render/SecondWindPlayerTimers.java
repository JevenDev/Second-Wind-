package com.jvn.secondwind.client.render;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.ClientTrackedDownedPlayers;
import com.jvn.secondwind.client.hud.SecondWindHud;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SecondWindPlayerTimers {
    private static final ResourceLocation SKULL_ICON = ToucanResourceLocations.id(SecondWindMod.MOD_ID, "icons/skull.png");
    private static final float LABEL_SCALE = 0.025F;
    private static final float ICON_SIZE = 10.0F;
    private static final float ICON_GAP = 3.0F;

    private SecondWindPlayerTimers() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(SecondWindPlayerTimers::render);
    }

    private static void render(WorldRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || context.consumers() == null) {
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (!ClientTrackedDownedPlayers.isDowned(player.getId())) {
                continue;
            }

            Vec3 nameTagOffset = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getViewYRot(context.tickCounter().getGameTimeDeltaPartialTick(false)));
            if (nameTagOffset == null) {
                continue;
            }

            renderTimer(context, minecraft, player, player.getDisplayName().copy().withStyle(ChatFormatting.RED), nameTagOffset);
        }
    }

    private static void renderTimer(
            WorldRenderContext context,
            Minecraft minecraft,
            Player player,
            Component nameTagContent,
            Vec3 nameTagOffset) {
        float displayedTicksRemaining = ClientTrackedDownedPlayers.displayedTicksRemaining(player.getId());
        String timerLabel = SecondWindHud.formatTimerLabel(displayedTicksRemaining);
        int timerColor = SecondWindHud.timerTextColor(displayedTicksRemaining);
        Font font = minecraft.font;
        float textWidth = font.width(timerLabel);
        float contentWidth = ICON_SIZE + ICON_GAP + textWidth;
        float iconLeft = -contentWidth / 2.0F;
        float textX = iconLeft + ICON_SIZE + ICON_GAP;
        int vanillaNameY = "deadmau5".equals(nameTagContent.getString()) ? -10 : 0;
        float textY = vanillaNameY - font.lineHeight - 4.0F;
        float iconTop = textY + (font.lineHeight - ICON_SIZE) / 2.0F - 1.0F;
        float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 camera = context.camera().getPosition();
        Vec3 position = player.getPosition(tickDelta);
        double x = position.x - camera.x + nameTagOffset.x;
        double y = position.y - camera.y + nameTagOffset.y + 0.5D;
        double z = position.z - camera.z + nameTagOffset.z;

        PoseStack poseStack = context.matrixStack();
        MultiBufferSource bufferSource = context.consumers();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        drawSkull(bufferSource, matrix, iconLeft, iconTop, !player.isDiscrete());
        font.drawInBatch8xOutline(
                Component.literal(timerLabel).getVisualOrderText(),
                textX,
                textY,
                timerColor,
                SecondWindHud.TIMER_OUTLINE_COLOR,
                matrix,
                bufferSource,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void drawSkull(MultiBufferSource bufferSource, Matrix4f matrix, float x, float y, boolean seeThrough) {
        VertexConsumer consumer = bufferSource.getBuffer(seeThrough ? RenderType.textSeeThrough(SKULL_ICON) : RenderType.text(SKULL_ICON));
        addVertex(consumer, matrix, x, y + ICON_SIZE, 0.0F, 1.0F);
        addVertex(consumer, matrix, x + ICON_SIZE, y + ICON_SIZE, 1.0F, 1.0F);
        addVertex(consumer, matrix, x + ICON_SIZE, y, 1.0F, 0.0F);
        addVertex(consumer, matrix, x, y, 0.0F, 0.0F);
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float u, float v) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
