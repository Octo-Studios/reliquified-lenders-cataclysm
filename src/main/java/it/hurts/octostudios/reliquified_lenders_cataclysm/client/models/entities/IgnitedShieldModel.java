package it.hurts.octostudios.reliquified_lenders_cataclysm.client.models.entities;

import com.github.L_Ender.lionfishapi.client.model.tools.AdvancedEntityModel;
import com.github.L_Ender.lionfishapi.client.model.tools.AdvancedModelBox;
import com.github.L_Ender.lionfishapi.client.model.tools.BasicModelPart;
import com.google.common.collect.ImmutableList;
import it.hurts.octostudios.reliquified_lenders_cataclysm.entities.relics.fire_plate.IgnitedShieldEntity;
import org.jetbrains.annotations.NotNull;

public class IgnitedShieldModel extends AdvancedEntityModel<IgnitedShieldEntity> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox shieldJoint;
    private final AdvancedModelBox shield;

    /**
     * This model is based on L_Ender's Ignited Revenant model
     */
    public IgnitedShieldModel() {
        texWidth = 128;
        texHeight = 128;

        root = new AdvancedModelBox(this);
        root.setRotationPoint(0.0F, 24.0F, 0.0F);

        shieldJoint = new AdvancedModelBox(this);
        shieldJoint.setRotationPoint(0.0F, 0.0F, 0.0F);
        setRotationAngle(shieldJoint, 0.0F, -0.7854F, 0.0F);

        shield = new AdvancedModelBox(this);
        shield.setTextureOffset(33, 0).addBox(-3.5F, -3.0F, -1.0F, 7.0F, 21.0F, 1.0F, 0.0F, false);
        shield.setTextureOffset(69, 61).addBox(-4.0F, -5.0F, -1.5F, 8.0F, 5.0F, 2.0F, 0.0F, false);
        shield.setTextureOffset(63, 15).addBox(-4.0F, 15.0F, -1.25F, 8.0F, 5.0F, 2.0F, 0.0F, false);
        shield.setTextureOffset(34, 66).addBox(-3.0F, 2.0F, -1.5F, 6.0F, 12.0F, 0.0F, 0.0F, false);
        shield.setRotationPoint(0.0F, -4.8F, -12.3F);
        setRotationAngle(shield, -0.2182F, 0.0F, 0.0F);

        AdvancedModelBox rightParts = new AdvancedModelBox(this);
        rightParts.setTextureOffset(60, 23).addBox(-4.0F, -7.0F, -1.0F, 5.0F, 21.0F, 1.0F, 0.0F, false);
        rightParts.setTextureOffset(50, 0).addBox(-4.0F, -7.0F, -1.0F, 5.0F, 21.0F, 1.0F, 0.3F, false);
        rightParts.setTextureOffset(72, 0).addBox(-4.25F, -8.5F, -1.5F, 7.0F, 4.0F, 2.0F, 0.0F, false);
        rightParts.setTextureOffset(72, 0).addBox(-4.25F, 11.5F, -1.5F, 7.0F, 4.0F, 2.0F, 0.0F, false);
        rightParts.setRotationPoint(-3.5F, 4.0F, 0.5F);
        setRotationAngle(rightParts, 0.0436F, 0.0436F, -0.0873F);

        AdvancedModelBox leftParts = new AdvancedModelBox(this);
        leftParts.setTextureOffset(47, 43).addBox(-1.0F, -7.0F, -1.0F, 5.0F, 21.0F, 1.0F, 0.0F, false);
        leftParts.setTextureOffset(34, 43).addBox(-1.0F, -7.0F, -1.0F, 5.0F, 21.0F, 1.0F, 0.3F, false);
        leftParts.setTextureOffset(70, 46).addBox(-2.75F, -8.5F, -1.5F, 7.0F, 4.0F, 2.0F, 0.0F, false);
        leftParts.setTextureOffset(69, 69).addBox(-2.75F, 11.5F, -1.5F, 7.0F, 4.0F, 2.0F, 0.0F, false);
        leftParts.setRotationPoint(3.5F, 4.0F, 0.5F);
        setRotationAngle(leftParts, 0.0436F, -0.0436F, 0.0873F);

        AdvancedModelBox spikeRight = new AdvancedModelBox(this);
        spikeRight.setTextureOffset(63, 0).addBox(0.0F, -12.5F, -8.0F, 0.0F, 6.0F, 8.0F, 0.0F, false);
        spikeRight.setTextureOffset(61, 46).addBox(0.0F, 8.5F, -8.0F, 0.0F, 6.0F, 8.0F, 0.0F, false);
        spikeRight.setRotationPoint(4.0F, 6.5F, -1.0F);
        setRotationAngle(spikeRight, 0.0F, -0.3491F, 0.0F);

        AdvancedModelBox spikeLeft = new AdvancedModelBox(this);
        spikeLeft.setTextureOffset(0, 61).addBox(0.0F, -12.5F, -8.0F, 0.0F, 6.0F, 8.0F, 0.0F, false);
        spikeLeft.setTextureOffset(52, 58).addBox(0.0F, 8.5F, -8.0F, 0.0F, 6.0F, 8.0F, 0.0F, false);
        spikeLeft.setRotationPoint(-4.0F, 6.5F, -1.0F);
        setRotationAngle(spikeLeft, 0.0F, 0.3491F, 0.0F);

        root.addChild(shieldJoint);
        shieldJoint.addChild(shield);
        shield.addChild(rightParts);
        shield.addChild(leftParts);
        shield.addChild(spikeRight);
        shield.addChild(spikeLeft);

        updateDefaultPose();
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, shieldJoint, shield);
    }

    @Override
    public Iterable<BasicModelPart> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public void setupAnim(@NotNull IgnitedShieldEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    private void setRotationAngle(AdvancedModelBox AdvancedModelBox, float x, float y, float z) {
        AdvancedModelBox.rotateAngleX = x;
        AdvancedModelBox.rotateAngleY = y;
        AdvancedModelBox.rotateAngleZ = z;
    }
}


