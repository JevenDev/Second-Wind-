package com.jvn.secondwind.client.render;

import com.jvn.secondwind.SecondWindMod;
import com.jvn.secondwind.client.ClientTrackedDownedPlayers;
import com.jvn.secondwind.client.hud.SecondWindHud;
import com.jvn.toucanlib.util.ToucanResourceLocations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = SecondWindMod.MOD_ID, value = Dist.CLIENT)
public final class SecondWindPlayerTimers {
    private static final ResourceLocation SKULL_ICON = ToucanResourceLocations.id(SecondWindMod.MOD_ID, "icons/skull.png");
    private static final float LABEL_SCALE = 0.025F;
    private static final float ICON_SIZE = 10.0F;
    private static final float ICON_GAP = 3.0F;
    private SecondWindPlayerTimers() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !(event.getEntity() instanceof LivingEntity player)) {
            return;
        }

        if (!ClientTrackedDownedPlayers.isDowned(player.getId()) || !ClientTrackedDownedPlayers.timerVisible(player.getId())) {
            return;
        }

        event.setContent(event.getContent().copy().withStyle(ChatFormatting.RED));

        Vec3 nameTagOffset = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getViewYRot(event.getPartialTick()));
        if (nameTagOffset == null) {
            return;
        }

        renderTimer(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                minecraft,
                player,
                event.getContent(),
                nameTagOffset,
                event.getPackedLight());
    }

    private static void renderTimer(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Minecraft minecraft,
            LivingEntity player,
            Component nameTagContent,
            Vec3 nameTagOffset,
            int packedLight) {
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

        poseStack.pushPose();
        poseStack.translate(nameTagOffset.x, nameTagOffset.y + 0.5F, nameTagOffset.z);
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
                packedLight);
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
