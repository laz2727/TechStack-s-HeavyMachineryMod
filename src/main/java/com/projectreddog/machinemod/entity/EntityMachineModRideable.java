package com.projectreddog.machinemod.entity;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.projectreddog.machinemod.init.ModNetwork;
import com.projectreddog.machinemod.network.MachineModMessageEntityInventoryChangedToClient;
import com.projectreddog.machinemod.network.MachineModMessageEntityToClient;
import com.projectreddog.machinemod.reference.Reference;
import com.projectreddog.machinemod.utility.LogHelper;

public class EntityMachineModRideable extends Entity implements IInventory {

	public double velocity;
	public float yaw;
	protected ItemStack[] inventory;

	public boolean isPlayerAccelerating = false;
	public boolean isPlayerBreaking = false;
	public boolean isPlayerTurningRight = false;
	public boolean isPlayerTurningLeft = false;
	public boolean isPlayerPushingSprintButton = false;
	public boolean isPlayerPushingJumpButton = false;
	public double TargetposX;
	public double TargetposY;
	public double TargetposZ;
	public float TargetYaw;

	public double lastPosX = 0d;
	public double lastPosY = 0d;
	public double lastPosZ = 0d;
	public float lastAttribute1 = 0f;
	public int sendInterval = 0;

	public float Attribute1;// multi-purpose variable use defined in extended

	public AxisAlignedBB BoundingBox;

	protected double mountedOffsetY = 0d;
	protected double mountedOffsetX = 0d;
	protected double mountedOffsetZ = 0d;
	protected float maxAngle = 0;
	protected float minAngle = 0;

	protected Item droppedItem = null;

	public EntityMachineModRideable(World world) {
		super(world);
		setSize(1.5F, 0.6F); // should be overridden in Extened version.
		this.stepHeight = 1;
		inventory = new ItemStack[0];

	}

	public double getMaxVelocity() {
		// created as method so extending class can easily override to allow for
		// different speeds per machine
		return 0.2d;
	}

	// 1.8
	// @Override
	// public AxisAlignedBB getBoundingBox(){
	// return this.BoundingBox;
	// }

	// 1.8
	// @Override
	// public AxisAlignedBB getCollisionBox(Entity entity){
	// if (entity != riddenByEntity){
	// return entity.boundingBox;
	// }
	// else{
	// return null;// do not colide with the rider
	// }
	// }

	@Override
	public boolean canBeCollidedWith() {
		return !isDead;
	}

	public Item getItemToBeDropped() {
		// need to drop additional items from the inventory of the item
		Random rand = new Random();

		for (int i = 0; i < this.getSizeInventory(); i++) {
			ItemStack item = this.getStackInSlot(i);

			if (item != null && item.stackSize > 0) {
				float rx = rand.nextFloat() * 0.8F + 0.1F;
				float ry = rand.nextFloat() * 0.8F + 0.1F;
				float rz = rand.nextFloat() * 0.8F + 0.1F;

				EntityItem entityItem = new EntityItem(worldObj, posX + rx, posY + ry, posZ + rz, item);

				if (item.hasTagCompound()) {
					entityItem.getEntityItem().setTagCompound((NBTTagCompound) item.getTagCompound().copy());
				}

				float factor = 0.05F;
				entityItem.motionX = rand.nextGaussian() * factor;
				entityItem.motionY = rand.nextGaussian() * factor + 0.2F;
				entityItem.motionZ = rand.nextGaussian() * factor;
				worldObj.spawnEntityInWorld(entityItem);
				// item.stackSize = 0;
				this.setInventorySlotContents(i, null);

			}
		}
		return droppedItem;

	}

	@Override
	public boolean interactFirst(EntityPlayer player) // should be proper class
	{
		if (!worldObj.isRemote && riddenByEntity == null) {
			// server side and no rider

			if (player.isSneaking()) {
				if (getItemToBeDropped() != null) {
					this.dropItem(getItemToBeDropped(), 1);
					this.setDead();
				}

			} else {
				player.mountEntity(this);
			}
		}
		return true;
	}

	@Override
	public double getMountedYOffset() {
		// should be overridden in extended class if not default;
		return this.height * mountedOffsetY;
	}

	public double getMountedXOffset() {
		// should be overridden in extended class if not default;
		return calcOffsetX(mountedOffsetX);
	}

	public double getMountedZOffset() {
		// should be overridden in extended class if not default;
		return calcOffsetZ(mountedOffsetZ);
	}

	public void updateServer() {

		// New for gravity
		// this.motionY -= 0.03999999910593033D;
		// if (onGround){
		//
		// this.motionY *= -0.5D;
		//
		// }
		lastPosX = posX;
		lastPosY = posY;
		lastPosZ = posZ;
		lastAttribute1 = Attribute1;
		if (posY < 0) {
			this.setDead();
		}

		if (worldObj.isAirBlock(new BlockPos((int) (posX - .5d), (int) posY, (int) (posZ - .5d)))) {
			// in air block so fall i'll actually park the entity inside the
			// block below just a little bit.
			this.motionY -= 0.03999999910593033D;

		} else {

			this.motionY = 0;
			this.posY = (int) this.posY + 1;
		}

		// end New for gravity

		if (riddenByEntity != null) {

			if (isPlayerAccelerating) {
				this.velocity += .1d;
			}
			if (isPlayerBreaking) {
				this.velocity -= .1d;
			}
			if (isPlayerTurningRight) {
				yaw += 1.5d;
			}
			if (isPlayerTurningLeft) {
				yaw -= 1.5d;
			}

		}
		if (isPlayerPushingJumpButton) {
			Attribute1 -= 1;
			if (Attribute1 < getMinAngle()) {
				Attribute1 = getMinAngle();
			}
		} else if (isPlayerPushingSprintButton) {
			Attribute1 += 1;
			if (Attribute1 > getMaxAngle()) {
				Attribute1 = getMaxAngle();
			}
		}

		// end take user input

		// Clamp values to max / min values as needed
		if (this.velocity > this.getMaxVelocity()) {
			this.velocity = this.getMaxVelocity();
		} else if (this.velocity < this.getMaxVelocity() * -1) {
			this.velocity = this.getMaxVelocity() * -1;
		}
		if (this.velocity < 0.0001d && this.velocity > 0.0d) {
			this.velocity = 0d;

		} else if (this.velocity > -0.0001d && this.velocity < 0.0d) {
			this.velocity = 0d;
		}
		if (this.yaw > 360) {
			this.yaw = this.yaw - 360;
		} else if (this.yaw < 0) {
			this.yaw = 360 - this.yaw;
		}
		// END Clamp values to max / min values as needed

		// calc x & Z offsets needed for the given rotation & velocity
		double speedX = (velocity * MathHelper.cos((float) ((yaw + 90) * Math.PI / 180.0D)));
		double speedZ = (velocity * MathHelper.sin((float) ((yaw + 90) * Math.PI / 180.0D)));
		// double speedY=0;

		motionX = speedX;
		motionZ = speedZ;
		this.velocity *= .90;// apply friction
		setRotation(this.yaw, this.rotationPitch);

		// motionY= speedY;
		// setPosition( posX+speedX,posY+motionY, posZ+speedZ);
		moveEntity(motionX, motionY, motionZ);
		//
		// if (lastPosX != posX || lastPosY != posY || lastPosZ != posZ ||
		// lastAttribute1 != Attribute1 || sendInterval > 9) {
		// // only send the packet if something has changed should reduce
		// // network traffic if the entity is idle(just sitting still)
		// LogHelper.info("SendPacket");

		ModNetwork.sendPacketToAllAround((new MachineModMessageEntityToClient(this.getEntityId(), this.posX, this.posY, this.posZ, this.yaw, this.Attribute1)), new TargetPoint(worldObj.provider.getDimensionId(), posX, posY, posZ, 80));
		// sendInterval = 0;
		// }
		//
		// sendInterval++;
		// ModNetwork.simpleNetworkWrapper.sendToAllAround((new
		// MachineModMessageEntityToClient(
		// this.getEntityId(),this.posX,this.posY,this.posZ,this.yaw,this.Attribute1)),
		// new TargetPoint(worldObj.provider.getDimensionId(), posX, posY, posZ,
		// 80));
	}

	public void updateClient() {
		// updateServer();

		// this.noClip = true;
		this.motionX = 0;
		this.motionY = 0;
		this.motionZ = 0;

		//
		// if(TargetYaw!=yaw){
		// YawTickCount=3;
		// }
		//

		this.motionX = (this.TargetposX - this.posX) / (3);
		this.motionY = (this.TargetposY - this.posY) / (3);
		this.motionZ = (this.TargetposZ - this.posZ) / (3);
		if (this.motionX > 1 || this.motionY > 1 || this.motionZ > 1) {
			// in cases of desync override the smoothing effect and just put the
			// entity in place
			// this resolves the issue of the entity jumping after placed.
			this.motionX *= 3;
			this.motionY *= 3;
			this.motionZ *= 3;
		}

		setPosition(posX + motionX, posY + motionY, posZ + motionZ);

		//LogHelper.info("Client: isinvis:" + this.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer) + " DIMID:" + worldObj.provider.getDimensionId() + " X:" + posX + "         Y:" + posY + "       Z:" + posZ);
		//
		//
		// if(YawTickCount>0){
		//
		// this.rotationYaw += (TargetYaw -yaw)/YawTickCount;
		// YawTickCount--;
		// }
		// if (YawTickCount==0){
		// this.rotationYaw=TargetYaw;
		// }
	}

	@Override
	@SideOnly(Side.CLIENT)
	// override the set position and rotation function to avoid MC from setting
	// the postion of the entity so i can handle it
	// in my network handler ... avoids jitter
	public void func_180426_a(double p_70056_1_, double p_70056_3_, double p_70056_5_, float p_70056_7_, float p_70056_8_, int p_70056_9_, boolean bool) {

	}

	// @SideOnly(Side.CLIENT)
	// public void setPositionAndRotation2(double p_70056_1_, double p_70056_3_,
	// double p_70056_5_, float p_70056_7_, float p_70056_8_, int p_70056_9_) {
	//
	// }
	//
	// @SideOnly(Side.CLIENT)
	// public void setPositionAndRotation(double p_70056_1_, double p_70056_3_,
	// double p_70056_5_, float p_70056_7_, float p_70056_8_, int p_70056_9_) {
	//
	// }
	@Override
	public void onUpdate() {
		super.onUpdate();
		if (!worldObj.isRemote) {
			// server side
			updateServer();

		} else {
			// client

			updateClient();
		}

	}

	public void updateRiderPosition() {
		if (this.riddenByEntity != null) {
			double d0 = Math.cos((double) this.rotationYaw * Math.PI / 180.0D) * this.velocity;
			double d1 = Math.sin((double) this.rotationYaw * Math.PI / 180.0D) * this.velocity;
			this.riddenByEntity.setPosition(this.posX + d0 + this.getMountedXOffset(), this.posY + this.getMountedYOffset() + this.riddenByEntity.getYOffset(), this.posZ + d1 + this.getMountedZOffset());
			// this.riddenByEntity.setRotationYawHead(this.yaw);
		}
	}

	@Override
	protected void entityInit() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		// TODO Auto-generated method stub

		// super.readFromNBT(compound);
		// no need to call super it calls this method instead.

		yaw = compound.getFloat(Reference.MACHINE_MOD_NBT_PREFIX + "YAW");
		velocity = compound.getDouble(Reference.MACHINE_MOD_NBT_PREFIX + "VELOCITY");
		TargetposX = compound.getDouble(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_X");
		TargetposY = compound.getDouble(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_Y");
		TargetposZ = compound.getDouble(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_Z");
		TargetYaw = compound.getFloat(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_YAW");
		Attribute1 = compound.getFloat(Reference.MACHINE_MOD_NBT_PREFIX + "ATTRIBUTE1");

		// inventory
		NBTTagList tagList = compound.getTagList(Reference.MACHINE_MOD_NBT_PREFIX + "Inventory", compound.getId());
		for (int i = 0; i < tagList.tagCount(); i++) {
			NBTTagCompound tag = (NBTTagCompound) tagList.getCompoundTagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < inventory.length) {
				inventory[slot] = ItemStack.loadItemStackFromNBT(tag);
			}
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		// super.writeToNBT(compound);
		// no need to call the super it calls this method instead
		compound.setFloat(Reference.MACHINE_MOD_NBT_PREFIX + "YAW", yaw);
		compound.setDouble(Reference.MACHINE_MOD_NBT_PREFIX + "VELOCITY", velocity);
		compound.setDouble(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_X", TargetposX);
		compound.setDouble(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_Y", TargetposY);
		compound.setDouble(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_Z", TargetposZ);
		compound.setFloat(Reference.MACHINE_MOD_NBT_PREFIX + "TARGET_YAW", TargetYaw);
		compound.setFloat(Reference.MACHINE_MOD_NBT_PREFIX + "ATTRIBUTE1", Attribute1);

		// inventory
		NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < inventory.length; i++) {
			ItemStack stack = inventory[i];
			if (stack != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				stack.writeToNBT(tag);
				itemList.appendTag(tag);
			}
		}
		compound.setTag(Reference.MACHINE_MOD_NBT_PREFIX + "Inventory", itemList);

	}

	public float getMaxAngle() {
		return this.maxAngle;
	}

	public float getMinAngle() {
		return this.minAngle;
	}

	public double calcOffsetX(double distance) {
		return (distance * MathHelper.cos((float) (clampAngelto360(this.yaw + 90) * Math.PI / 180.0D)));
	}

	public double calcOffsetZ(double distance) {
		return (distance * MathHelper.sin((float) (clampAngelto360(this.yaw + 90) * Math.PI / 180.0D)));

	}

	public double calcTwoOffsetX(double distance, int secondOffsetAngle, double secondOffsetDistance) {

		if (secondOffsetAngle == 0) {
			return (calcOffsetX(distance));
		}
		// calc first xPos
		double firstX = calcOffsetX(distance);

		return firstX + (secondOffsetDistance * MathHelper.cos((float) (clampAngelto360(this.yaw + secondOffsetAngle + 90) * Math.PI / 180.0D)));
	}

	public double calcTwoOffsetZ(double distance, int secondOffsetAngle, double secondOffsetDistance) {

		if (secondOffsetAngle == 0) {
			return (calcOffsetZ(distance));
		}
		// calc first xPos
		double firstZ = calcOffsetZ(distance);

		return firstZ + (secondOffsetDistance * MathHelper.sin((float) (clampAngelto360(this.yaw + secondOffsetAngle + 90) * Math.PI / 180.0D)));
	}

	public float clampAngelto360(float inAngle) {
		while (inAngle > 360) {
			inAngle -= 360;
		}

		while (inAngle < 0) {
			inAngle = 360 - inAngle;
		}
		return inAngle;
	}

	public void toppleTree(BlockPos bp, int depth) {
		if (depth < Reference.MAX_TREE_DEPTH) {
			if (worldObj.getBlockState(bp).getBlock() == Blocks.log || worldObj.getBlockState(bp).getBlock() == Blocks.log2 || worldObj.getBlockState(bp).getBlock() == Blocks.leaves || worldObj.getBlockState(bp).getBlock() == Blocks.leaves2) {

				worldObj.getBlockState(bp).getBlock().dropBlockAsItem(worldObj, bp, worldObj.getBlockState(bp), 0);
				worldObj.setBlockToAir(bp);
				toppleTree(bp.offset(EnumFacing.DOWN), depth + 1);
				toppleTree(bp.offset(EnumFacing.UP), depth + 1);
				toppleTree(bp.offset(EnumFacing.SOUTH), depth + 1);
				toppleTree(bp.offset(EnumFacing.EAST), depth + 1);
				toppleTree(bp.offset(EnumFacing.WEST), depth + 1);
				toppleTree(bp.offset(EnumFacing.NORTH), depth + 1);

			}
		}
	}

	// adds to this objects inventory if it can
	// any remaining amount will be returned
	protected ItemStack addToinventory(ItemStack is) {
		int i = getSizeInventory();

		for (int j = 0; j < i && is != null && is.stackSize > 0; ++j) {
			if (is != null) {

				if (getStackInSlot(j) != null) {
					if (getStackInSlot(j).getItem() == is.getItem() && getStackInSlot(j).getItemDamage() == is.getItemDamage()) {
						// same item remove from is put into slot any amt not to
						// excede stack max
						if (getStackInSlot(j).stackSize < getStackInSlot(j).getMaxStackSize()) {
							// we have room to add to this stack
							if (is.stackSize <= getStackInSlot(j).getMaxStackSize() - getStackInSlot(j).stackSize) {
								// /all of the stack will fit in this slot do
								// so.

								setInventorySlotContents(j, new ItemStack(getStackInSlot(j).getItem(), getStackInSlot(j).stackSize + is.stackSize, is.getItemDamage()));
								is = null;
							} else {
								// we have more
								int countRemain = is.stackSize - (getStackInSlot(j).getMaxStackSize() - getStackInSlot(j).stackSize);
								setInventorySlotContents(j, new ItemStack(is.getItem(), getStackInSlot(j).getMaxStackSize(), is.getItemDamage()));
								is.stackSize = countRemain;
							}

						}
					}
				} else {
					// nothign in slot so set contents
					setInventorySlotContents(j, new ItemStack(is.getItem(), is.stackSize, is.getItemDamage()));
					is = null;
				}

			}

		}

		return null;

	}

	@Override
	public int getSizeInventory() {
		return inventory.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventory[slot];
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		inventory[slot] = stack;
		if (stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}
		if (!(this.worldObj.isRemote)) {
			// send packet to notify client of contents of machine's inventory
			ModNetwork.sendPacketToAllAround((new MachineModMessageEntityInventoryChangedToClient(this.getEntityId(), slot, inventory[slot])), new TargetPoint(worldObj.provider.getDimensionId(), posX, posY, posZ, 80));
		}

	}

	@Override
	public void markDirty() {
		// TODO Auto-generated method stub

	}

	@Override
	public void openInventory(EntityPlayer playerIn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void closeInventory(EntityPlayer playerIn) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		// check if the player is near the entity.
		return player.getDistanceSq(posX, posY, posZ) < 64;
	}

	@Override
	public ItemStack decrStackSize(int slot, int amt) {
		ItemStack stack = getStackInSlot(slot);
		if (stack != null) {
			if (stack.stackSize <= amt) {
				setInventorySlotContents(slot, null);
			} else {
				stack = stack.splitStack(amt);
				if (stack.stackSize == 0) {
					setInventorySlotContents(slot, null);
				}

			}
		}
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		ItemStack stack = getStackInSlot(slot);
		if (stack != null) {
			setInventorySlotContents(slot, null);
		}
		return stack;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {

	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		for (int i = 0; i < inventory.length; ++i) {
			inventory[i] = null;
		}
	}
}
