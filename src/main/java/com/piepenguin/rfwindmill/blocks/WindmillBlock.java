package com.piepenguin.rfwindmill.blocks;

import com.piepenguin.rfwindmill.lib.Constants;
import com.piepenguin.rfwindmill.lib.EnergyStorage;
import com.piepenguin.rfwindmill.lib.Lang;
import com.piepenguin.rfwindmill.lib.Util;
import com.piepenguin.rfwindmill.tileentities.TileEntityWindmillBlock;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class WindmillBlock extends Block implements ITileEntityProvider {

    protected final int maximumEnergyGeneration;
    protected final int maximumEnergyTransfer;
    protected final int capacity;

    private String name;
    private IIcon frontIcon;
    private IIcon sideIcon;

    public WindmillBlock(String pName, int pMaximumEnergyGeneration, int pCapacity) {
        super(Material.rock);
        setHardness(3.5f);
        setStepSound(Block.soundTypeMetal);
        maximumEnergyGeneration = pMaximumEnergyGeneration;
        maximumEnergyTransfer = pMaximumEnergyGeneration * 40;
        capacity = pCapacity;
        name = pName;
        this.setBlockName(Constants.MODID + "_" + name);
        this.setCreativeTab(CreativeTabs.tabBlock);
        GameRegistry.registerBlock(this, name);
    }

    @Override
    public void registerBlockIcons(IIconRegister pIconRegister) {
        sideIcon = pIconRegister.registerIcon(Constants.MODID + ":" + name + "Side");
        frontIcon = pIconRegister.registerIcon(Constants.MODID + ":" + name + "Front");
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess pWorld, int pX, int pY, int pZ, int pSide) {
        TileEntityWindmillBlock entity = (TileEntityWindmillBlock)pWorld.getTileEntity(pX, pY, pZ);
        int orientation = entity.getOrientation();
        switch(pSide) {
            case 0: case 1:
                return sideIcon;
            case 2: case 3:
                return (orientation == 0 || orientation == 2) ? frontIcon : sideIcon;
            case 4: case 5:
                return (orientation == 1 || orientation == 3) ? frontIcon : sideIcon;
            default:
                return sideIcon;
        }
    }
    @Override
    public TileEntity createNewTileEntity(World pWorld, int pMeta) {
        return new TileEntityWindmillBlock(maximumEnergyGeneration, maximumEnergyTransfer, capacity);
    }

    @Override
    public boolean hasTileEntity(int pMetadata) {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World pWorld, int pX, int pY, int pZ, EntityLivingBase pEntity, ItemStack pItemStack) {
        int direction = MathHelper.floor_double((double) (pEntity.rotationYaw * 4.0f / 360.0f) + 0.50) & 3;
        TileEntityWindmillBlock entity = (TileEntityWindmillBlock)pWorld.getTileEntity(pX, pY, pZ);
        entity.setOrientation(direction);
        if(pItemStack.stackTagCompound != null) {
            entity.setEnergyStored(pItemStack.stackTagCompound.getInteger(EnergyStorage.NBT_ENERGY));
        }
        super.onBlockPlacedBy(pWorld, pX, pY, pZ, pEntity, pItemStack);
    }

    @Override
    public boolean onBlockActivated(World pWorld, int pX, int pY, int pZ, EntityPlayer pPlayer, int pSide, float pDx, float pDy, float pDz) {
        if(!pWorld.isRemote) {
            if(pPlayer.isSneaking()) {
                // Dismantle block if player has a wrench
                if(Util.hasWrench(pPlayer, pX, pY, pZ)) {
                    dismantle(pWorld, pX, pY, pZ);
                    return true;
                }
                else {
                    // Print energy information otherwise
                    printChatInfo(pWorld, pX, pY, pZ, pPlayer);
                    return true;
                }
            }
            else {
                if(Util.hasWrench(pPlayer, pX, pY, pZ)) {
                    // Orient the block to face the player
                    int direction = MathHelper.floor_double((double) (pPlayer.rotationYaw * 4.0f / 360.0f) + 0.50) & 3;
                    TileEntityWindmillBlock entity = (TileEntityWindmillBlock)pWorld.getTileEntity(pX, pY, pZ);
                    entity.setOrientation(direction);
                    Minecraft.getMinecraft().renderGlobal.markBlockForRenderUpdate(pX, pY, pZ);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onBlockHarvested(World pWorld, int pX, int pY, int pZ, int pSide, EntityPlayer pPlayer) {
        if(!pWorld.isRemote) {
            dismantle(pWorld, pX, pY, pZ);
        }
    }

    private void dismantle(World pWorld, int pX, int pY, int pZ) {
        ItemStack itemStack = new ItemStack(this);

        TileEntityWindmillBlock entity = (TileEntityWindmillBlock)pWorld.getTileEntity(pX, pY, pZ);
        int energy = entity.getEnergyStored();
        if(energy > 0) {
            if(itemStack.getTagCompound() == null) {
                itemStack.setTagCompound(new NBTTagCompound());
            }
            itemStack.getTagCompound().setInteger(EnergyStorage.NBT_ENERGY, energy);
        }
        pWorld.setBlockToAir(pX, pY, pZ);
        EntityItem entityItem = new EntityItem(pWorld, pX + 0.5, pY + 0.5, pZ + 0.5, itemStack);
        entityItem.motionX = 0;
        entityItem.motionZ = 0;
        pWorld.spawnEntityInWorld(entityItem);
    }

    private void printChatInfo(World pWorld, int pX, int pY, int pZ, EntityPlayer pPlayer) {
        TileEntityWindmillBlock entity = (TileEntityWindmillBlock)pWorld.getTileEntity(pX, pY, pZ);
        String msg = String.format("%s: %d/%d RF",
                Lang.localise("energy.stored"),
                entity.getEnergyStored(),
                entity.getMaxEnergyStored());
        pPlayer.addChatMessage(new ChatComponentText(msg));
    }
}
