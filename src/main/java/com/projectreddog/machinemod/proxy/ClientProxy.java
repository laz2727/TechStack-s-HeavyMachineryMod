package com.projectreddog.machinemod.proxy;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import com.projectreddog.machinemod.entity.EntityBulldozer;
import com.projectreddog.machinemod.entity.EntityDumpTruck;
import com.projectreddog.machinemod.entity.EntityLoader;
import com.projectreddog.machinemod.entity.EntityTractor;
import com.projectreddog.machinemod.init.ModBlocks;
import com.projectreddog.machinemod.init.ModItems;
import com.projectreddog.machinemod.render.RenderBulldozer;
import com.projectreddog.machinemod.render.RenderDumpTruck;
import com.projectreddog.machinemod.render.RenderLoader;
import com.projectreddog.machinemod.render.RenderTractor;

public class ClientProxy extends CommonProxy {
	@Override
	public void registerRenderers() {

		// LogHelper.info("in register Renderers");

		RenderingRegistry.registerEntityRenderingHandler(EntityBulldozer.class, new RenderBulldozer(Minecraft.getMinecraft().getRenderManager()));
		// RenderingRegistry.registerEntityRenderingHandler(
		// EntityDrillingRig.class, new RenderDrillingRig(Minecraft
		// .getMinecraft().getRenderManager()));
		RenderingRegistry.registerEntityRenderingHandler(EntityDumpTruck.class, new RenderDumpTruck(Minecraft.getMinecraft().getRenderManager()));
		RenderingRegistry.registerEntityRenderingHandler(EntityLoader.class, new RenderLoader(Minecraft.getMinecraft().getRenderManager()));
		RenderingRegistry.registerEntityRenderingHandler(EntityTractor.class, new RenderTractor(Minecraft.getMinecraft().getRenderManager()));

		ModBlocks.initBlockRender();
		ModItems.initItemRender();
	}

}
