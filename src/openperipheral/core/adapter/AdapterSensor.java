package openperipheral.core.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import openperipheral.api.Arg;
import openperipheral.api.IPeripheralAdapter;
import openperipheral.api.LuaMethod;
import openperipheral.api.LuaType;
import openperipheral.core.interfaces.ISensorEnvironment;
import openperipheral.core.util.EntityUtils;
import openperipheral.turtle.TurtleSensorEnvironment;
import dan200.computer.api.IComputerAccess;
import dan200.turtle.api.ITurtleAccess;

public class AdapterSensor implements IPeripheralAdapter {

	@Override
	public Class getTargetClass() {
		return ISensorEnvironment.class;
	}

	private AxisAlignedBB getBoundingBox(Vec3 location, double range) {
		return AxisAlignedBB.getAABBPool().getAABB(location.xCoord, location.yCoord, location.zCoord, location.xCoord + 1, location.yCoord + 1, location.zCoord + 1).expand(range, range, range);
	}

	@LuaMethod(returnType = LuaType.TABLE, onTick = false, description = "Get the usernames of all the players in range")
	public ArrayList<String> getPlayerNames(IComputerAccess computer, ISensorEnvironment env) {
		List<EntityPlayer> players = env.getWorld().getEntitiesWithinAABB(EntityPlayer.class, getBoundingBox(env.getLocation(), env.getSensorRange()));
		ArrayList<String> names = new ArrayList<String>();
		for (EntityPlayer player : players) {
			names.add(player.username);
		}
		return names;
	}

	@LuaMethod(
		returnType = LuaType.TABLE, onTick = false, description = "Get full details of a particular player if they're in range",
		args = {
			@Arg(type = LuaType.STRING, name = "username", description = "The players username") })
	public Map getPlayerData(IComputerAccess computer, ISensorEnvironment env, String username) {
		ArrayList<String> surroundingPlayers = getPlayerNames(computer, env);
		if (surroundingPlayers.contains(username)) {
			EntityPlayer player = env.getWorld().getPlayerEntityByName(username);
			return EntityUtils.entityToMap(player, env.getLocation());
		}
		return null;
	}

	@LuaMethod(returnType = LuaType.TABLE, onTick = false, description = "Get the ids of all the mobs in range")
	public ArrayList<Integer> getMobIds(IComputerAccess computer, ISensorEnvironment env) {
		List<EntityLiving> mobs = env.getWorld().getEntitiesWithinAABB(EntityLiving.class, getBoundingBox(env.getLocation(), env.getSensorRange()));
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (EntityLiving mob : mobs) {
			ids.add(mob.entityId);
		}
		return ids;
	}

	@LuaMethod(
		returnType = LuaType.TABLE, onTick = false, description = "Get full details of a particular mob if it's in range",
		args = {
			@Arg(type = LuaType.NUMBER, name = "mobId", description = "The mob id retrieved from getMobIds()") })
	public Map getMobData(IComputerAccess computer, ISensorEnvironment sensor, int mobId) {
		ArrayList<Integer> surroundingMobs = getMobIds(computer, sensor);
		if (surroundingMobs.contains(mobId)) {
			Entity mob = sensor.getWorld().getEntityByID(mobId);
			return EntityUtils.entityToMap(mob, sensor.getLocation());
		}
		return null;
	}

	@LuaMethod(returnType = LuaType.TABLE, onTick = false, description = "Get the ids of all the minecarts in range")
	public ArrayList<Integer> getMinecartIds(IComputerAccess computer, ISensorEnvironment env) {
		List<EntityMinecart> minecarts = env.getWorld().getEntitiesWithinAABB(EntityMinecart.class, getBoundingBox(env.getLocation(), env.getSensorRange()));
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (EntityMinecart minecart : minecarts) {
			ids.add(minecart.entityId);
		}
		return ids;
	}

	@LuaMethod(returnType = LuaType.TABLE, onTick = false, description = "Get full details of a particular minecart if it's in range",
		args = {
			@Arg(type = LuaType.NUMBER, name = "minecartId", description = "The minecart id retrieved from getMobIds()") })
	public Map getMinecartData(IComputerAccess computer, ISensorEnvironment env, int minecartId) {
		ArrayList<Integer> surroundingCarts = getMinecartIds(computer, env);
		if (surroundingCarts.contains(minecartId)) {
			Entity cart = env.getWorld().getEntityByID(minecartId);
			return EntityUtils.entityToMap(cart, env.getLocation());
		}
		return null;
	}
	
	// Returns map 
	private Map sonicScanSingle(ISensorEnvironment env, int x, int y, int z) {
		World world = env.getWorld();
		String type = "UNKNOWN";
		String name = "UNKNOWN";

		int id = world.getBlockId(x, y, z);
		Block block = Block.blocksList[id];

		if (!(id == 0 || block == null)) {
			if (id == 0) {
				type = "AIR";
			} else if (block.blockMaterial.isLiquid()) {
				type = "LIQUID";
			} else if (block.blockMaterial.isSolid()) {
				type = "SOLID";
			}
			name = block.getLocalizedName();

		}
		HashMap tmp = new HashMap();
		tmp.put("type", type);
		tmp.put("name", name);
		return tmp;
	}
	
	@LuaMethod(returnType = LuaType.TABLE, onTick = false, description = "Get a table of information about the block in front of the turtle. Includes whether each block is UNKNOWN, AIR, LIQUID or SOLID")
	public Map scanAdjacent(IComputerAccess computer, ISensorEnvironment env) {
		if (!(env instanceof TurtleSensorEnvironment)) {
			return null;
		}

		ITurtleAccess turtle = ((TurtleSensorEnvironment) env).getTurtle();
		Vec3 pos = env.getLocation();
		int x = (int) pos.xCoord;
		int y = (int) pos.yCoord;
		int z = (int) pos.zCoord;
		
		Map status = new HashMap();
		status.put("dir", turtle.getFacingDir());
		status.put("x", x);
		status.put("y", y);
		status.put("z", z);
		
		Map results = new HashMap();
		results.put("status", status);
		results.put("east", sonicScanSingle(env, x + 1, y, z));
		results.put("north", sonicScanSingle(env, x, y, z + 1));
		results.put("west", sonicScanSingle(env, x - 1, y, z));
		results.put("south", sonicScanSingle(env, x, y, z - 1));

		return results;
	}

	
	@LuaMethod(returnType = LuaType.TABLE, onTick = false,
			description = "Get the current location.")
	public Map getLocation(IComputerAccess computer, ISensorEnvironment env) {
		Vec3 pos = env.getLocation();
		Map<String, Double> result = new HashMap<String, Double>();
		result.put("x", pos.xCoord);
		result.put("y", pos.yCoord);
		result.put("z", pos.zCoord);
		return ((Map) result);
	}
	
	// Returns the first slot number containing the item or -1 if not found.
	private int getSlotWithItem(ITurtleAccess turtle, int itemID) {
		for (int i = 0; i < turtle.getInventorySize(); i++) {
			ItemStack stack = turtle.getSlotContents(i);
			if (stack != null && stack.itemID == itemID && stack.stackSize > 0) {
				return i;
			}
		}
		return -1;
	}

	
	// The following four constants are returned by Turtle.getFacingDir().
	private static final int NORTH = 3;  // increase z
	private static final int SOUTH = 2;  // decrease z
	private static final int EAST = 5;  // increase x
	private static final int WEST = 4;  // decrease x
	// I made up the following two constants.
	private static final int UP = 6;  // increase y
	private static final int DOWN = 7;  // decrease y
	
	private Vec3 getFacedPosition(ISensorEnvironment env, int dir) {
		int dx = 0;
		int dy = 0;
		int dz = 0;
		
		if (dir == SOUTH) {
			dz--;
		} else if (dir == NORTH) {
			dz++;
		} else if (dir == WEST) {
			dx--;
		} else if (dir == EAST) {
			dx++;
		} else if (dir == UP) {
			dy++;
		} else if (dir == DOWN) {
			dy--;
		}
		
		return env.getLocation().addVector(dx, dy, dz);
	}
	
	private String detectBlockInner(ISensorEnvironment env, int dir) {
		if (!(env instanceof TurtleSensorEnvironment)) {
			return null;
		}
		Vec3 pos = getFacedPosition(env, dir);
		int id = env.getWorld().getBlockId(
				(int) pos.xCoord, (int) pos.yCoord, (int) pos.zCoord);
		Block block = Block.blocksList[id];
		if (id == 0 || block == null) {
			return "none";
		} else {
			return block.getLocalizedName();
		}
	}
	
	@LuaMethod(returnType = LuaType.STRING, onTick = false, 
			description = "Get the type of the block in front of the turtle.")
	public String detectBlock(IComputerAccess computer, ISensorEnvironment env) {
		if (!(env instanceof TurtleSensorEnvironment)) {
			return null;
		}
		ITurtleAccess turtle = ((TurtleSensorEnvironment) env).getTurtle();
		return turtle.getFacingDir() + ": " + detectBlockInner(env, turtle.getFacingDir());
	}
	
	@LuaMethod(returnType = LuaType.STRING, onTick = false, 
			description = "Get the type of the block above the turtle.")
	public String detectBlockUp(IComputerAccess computer, ISensorEnvironment env) {
		return detectBlockInner(env, UP);
	}
	
	@LuaMethod(returnType = LuaType.STRING, onTick = false, 
			description = "Get the type of the block above the turtle.")
	public String detectBlockDown(IComputerAccess computer, ISensorEnvironment env) {
		return detectBlockInner(env, DOWN);
	}
	
	@LuaMethod(returnType = LuaType.TABLE, onTick = false, description = "Get a table of information about the surrounding area. Includes whether each block is UNKNOWN, AIR, LIQUID or SOLID")
	public Map sonicScan(IComputerAccess computer, ISensorEnvironment env) {
		int range = 1 + (int)(env.getSensorRange() / 6);
		World world = env.getWorld();
		HashMap results = new HashMap();
		Vec3 sensorPos = env.getLocation();
		int sx = (int)sensorPos.xCoord;
		int sy = (int)sensorPos.yCoord;
		int sz = (int)sensorPos.zCoord;
		int i = 0;
		for (int x = -range; x <= range; x++) {
			for (int y = -range; y <= range; y++) {
				for (int z = -range; z <= range; z++) {
					Map tmp = null;

					if (!(x == 0 && y == 0 && z == 0) && 
						world.blockExists(sx + x, sy + y, sz + z)) {
						int bX = sx + x;
						int bY = sy + y;
						int bZ = sz + z;
						Vec3 targetPos = Vec3.createVectorHelper(bX, bY, bZ);
						if (sensorPos.distanceTo(targetPos) <= range) {
							tmp = sonicScanSingle(env, bX, bY, bZ);
						} else {
							continue;
						}
					}
					if (tmp == null) {
						tmp  = new HashMap();
						tmp.put("type", "UNKNOWN");
						tmp.put("name", "UNKNOWN");
					}
					tmp.put("x", x);
					tmp.put("y", y);
					tmp.put("z", z);
					if (env instanceof TurtleSensorEnvironment) {
						ITurtleAccess turtle = ((TurtleSensorEnvironment) env).getTurtle();
						tmp.put("dir", turtle.getFacingDir());
					}
					results.put(++i, tmp);
				}
			}
		}
		return results;
	}
	
	@LuaMethod(returnType = LuaType.NUMBER, onTick = false, 
			description = "Get the slot number of the specified item.",
			args = {@Arg(type=LuaType.NUMBER, name="item", description="The numeric identifier of the item")})
	public Integer getSlot(IComputerAccess computer, ISensorEnvironment env, int itemID) {
		if (!(env instanceof TurtleSensorEnvironment)) {
			return -1;
		}
		ITurtleAccess turtle = ((TurtleSensorEnvironment) env).getTurtle();
		return getSlotWithItem(turtle, itemID);
	}
	
	
	private String placeItemInner(ISensorEnvironment env, 
			int itemID, Vec3 pos) {
		if (!(env instanceof TurtleSensorEnvironment)) {
			return "not turtle";
		}
		ITurtleAccess turtle = ((TurtleSensorEnvironment) env).getTurtle();
		int slot = getSlotWithItem(turtle, itemID);
		if (slot == -1) {
			return "item not found";
		}
		ItemStack stack = turtle.getSlotContents(slot);
		// This only works client-side.
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		Boolean result = stack.tryPlaceItemIntoWorld(player, env.getWorld(),
				(int) pos.xCoord, (int) pos.yCoord, (int) pos.zCoord,
				4,  // arbitrary side
				// .5f gave me one block offs
				0f, 0f, 0f // arbitrary position on target block
				);
		return result.toString();
	}
	
	@LuaMethod(returnType = LuaType.STRING, onTick = false, 
			description = "Place the named item in front of the turtle.",
			args = {@Arg(type=LuaType.NUMBER, name="item", description="The numeric identifier of the item")})
	public String placeItem(IComputerAccess computer, ISensorEnvironment env, int itemID) {
		if (!(env instanceof TurtleSensorEnvironment)) {
			return "not turtle";
		}
		ITurtleAccess turtle = ((TurtleSensorEnvironment) env).getTurtle();
		Vec3 pos = getFacedPosition(env, turtle.getFacingDir());
		return placeItemInner(env, itemID, pos);
	}
}
