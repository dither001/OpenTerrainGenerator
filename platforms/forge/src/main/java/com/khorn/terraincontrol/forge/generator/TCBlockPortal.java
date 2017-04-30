package com.khorn.terraincontrol.forge.generator;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.lang.reflect.Field;

import com.khorn.terraincontrol.TerrainControl;
import com.khorn.terraincontrol.forge.ForgeEngine;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TCBlockPortal
{
    public static boolean trySpawnPortal(World worldIn, BlockPos pos, boolean isQuartz)
    {
    	boolean cartographerEnabled = ((ForgeEngine)TerrainControl.getEngine()).getCartographerEnabled();
    	
    	// Only create a portal when a TC custom dimension exists
    	boolean bFound = false;
    	boolean bFoundOtherThanCartographer = false;
		for(int i = 2; i < Long.SIZE << 4; i++)
		{
			if(DimensionManager.isDimensionRegistered(i))
			{
				DimensionType dimensionType = DimensionManager.getProviderType(i);
				if(dimensionType.getSuffix().equals("OTG"))
				{
					bFound = true;
					if(cartographerEnabled)
					{
						if(i != Cartographer.CartographerDimension)
						{
							bFoundOtherThanCartographer = true;
							break;
						}
					} else {
						break;
					}
				}
				//else if (i == 0 || i > 1)
				//{
					//throw new RuntimeException("Whatever it is you're trying to do, we didn't write any code for it (sorry). Please contact Team OTG about this crash.");
				//}
			}
		}
		if(!bFound)
		{
			return false;
		}
		
		// If Cartographer is on and this is a Cartographer portal and no other dimensions were found don't spawn a portal.
		IBlockState blockState = worldIn.getBlockState(pos);
		BlockPos firstSolidBlockPos = new BlockPos(pos);
		while(!blockState.getMaterial().isSolid() && firstSolidBlockPos.getY() > 0)
		{
			firstSolidBlockPos = new BlockPos(firstSolidBlockPos.getX(), firstSolidBlockPos.getY() - 1, firstSolidBlockPos.getZ());
			blockState = worldIn.getBlockState(firstSolidBlockPos);
		}
		boolean isCartographerPortal = blockState.getBlock() == Blocks.QUARTZ_BLOCK && (byte) blockState.getBlock().getMetaFromState(blockState) == 1;
		if(!(!cartographerEnabled && bFound) && (cartographerEnabled && !bFoundOtherThanCartographer && !isCartographerPortal))
		{
			return false;
		}
    	
        TCBlockPortal.Size blockportal$size = new TCBlockPortal.Size(worldIn, pos, EnumFacing.Axis.X, isQuartz);

        if (blockportal$size.isValid() && blockportal$size.portalBlockCount == 0)
        {
            blockportal$size.placePortalBlocks();
            return true;
        } else {
            TCBlockPortal.Size blockportal$size1 = new TCBlockPortal.Size(worldIn, pos, EnumFacing.Axis.Z, isQuartz);

            if (blockportal$size1.isValid() && blockportal$size1.portalBlockCount == 0)
            {
                blockportal$size1.placePortalBlocks();
                return true;
            } else {
                return false;
            }
        }
    }
    
    public static class Size
    {
        private final World world;
        private final EnumFacing.Axis axis;
        private final EnumFacing rightDir;
        private final EnumFacing leftDir;
        private int portalBlockCount;
        private BlockPos bottomLeft;
        private int height;
        private int width;

        public Size(World worldIn, BlockPos p_i45694_2_, EnumFacing.Axis p_i45694_3_, boolean isQuartz)
        {
            this.world = worldIn;
            this.axis = p_i45694_3_;

            if (p_i45694_3_ == EnumFacing.Axis.X)
            {
                this.leftDir = EnumFacing.EAST;
                this.rightDir = EnumFacing.WEST;
            }
            else
            {
                this.leftDir = EnumFacing.NORTH;
                this.rightDir = EnumFacing.SOUTH;
            }

            for (BlockPos blockpos = p_i45694_2_; p_i45694_2_.getY() > blockpos.getY() - 21 && p_i45694_2_.getY() > 0 && this.isEmptyBlock(worldIn.getBlockState(p_i45694_2_.down()).getBlock()); p_i45694_2_ = p_i45694_2_.down())
            {
                ;
            }

            int i = this.getDistanceUntilEdge(p_i45694_2_, this.leftDir, isQuartz) - 1;

            if (i >= 0)
            {
                this.bottomLeft = p_i45694_2_.offset(this.leftDir, i);
                this.width = this.getDistanceUntilEdge(this.bottomLeft, this.rightDir, isQuartz);

                if (this.width < 2 || this.width > 21)
                {
                    this.bottomLeft = null;
                    this.width = 0;
                }
            }

            if (this.bottomLeft != null)
            {
                this.height = this.calculatePortalHeight(isQuartz);
            }
        }

        protected int getDistanceUntilEdge(BlockPos p_180120_1_, EnumFacing p_180120_2_, boolean isQuartz)
        {
            int i;

            for (i = 0; i < 22; ++i)
            {
                BlockPos blockpos = p_180120_1_.offset(p_180120_2_, i);
                
                Block block = this.world.getBlockState(blockpos).getBlock();
                IBlockState blockStateDown = this.world.getBlockState(blockpos.down());
                Block blockDown = blockStateDown.getBlock();
               
                if (
            		!this.isEmptyBlock(block) ||
            		(
        				!isQuartz &&
        				blockDown != Blocks.OBSIDIAN
    				) ||
            		(
	            		isQuartz && 
	    				!(
							blockDown == Blocks.QUARTZ_BLOCK || 
							blockDown == Blocks.QUARTZ_STAIRS || 
							blockDown == Blocks.GLOWSTONE || 
							(
								(
									blockDown == Blocks.STONE_SLAB2 || 
									blockDown == Blocks.STONE_SLAB || 
									blockDown == Blocks.DOUBLE_STONE_SLAB2 ||
									blockDown == Blocks.DOUBLE_STONE_SLAB
								) && 
								(byte) blockStateDown.getBlock().getMetaFromState(blockStateDown) == 7
							)
						)
					)
				)
                {
                    break;
                }
            }

            IBlockState blockState = this.world.getBlockState(p_180120_1_.offset(p_180120_2_, i));
            Block block = blockState.getBlock();
            return 
    		(
    			(
					!isQuartz &&
					block == Blocks.OBSIDIAN
    			) ||
				(
	        		isQuartz && 
	        		(
	    				block == Blocks.QUARTZ_BLOCK || 
	    				block == Blocks.QUARTZ_STAIRS || 
	    				block == Blocks.GLOWSTONE || 
	    				(
							(
								block == Blocks.STONE_SLAB2 || 
								block == Blocks.STONE_SLAB || 
								block == Blocks.DOUBLE_STONE_SLAB2 || 
								block == Blocks.DOUBLE_STONE_SLAB
							) && 
							(byte) blockState.getBlock().getMetaFromState(blockState) == 7
						)
					)
				)
			) ? i : 0;
        }

        public int getHeight()
        {
            return this.height;
        }

        public int getWidth()
        {
            return this.width;
        }

        protected int calculatePortalHeight(boolean isQuartz)
        {
            label24:

            for (this.height = 0; this.height < 21; ++this.height)
            {
                for (int i = 0; i < this.width; ++i)
                {
                    BlockPos blockpos = this.bottomLeft.offset(this.rightDir, i).up(this.height);
                    Block block = this.world.getBlockState(blockpos).getBlock();

                    if (!this.isEmptyBlock(block))
                    {
                        break label24;
                    }

                    if (block == Blocks.PORTAL)
                    {
                        ++this.portalBlockCount;
                    }

                    if (i == 0)
                    {
                    	IBlockState blockState = this.world.getBlockState(blockpos.offset(this.leftDir));
                        block = blockState.getBlock();

                        if (
                    		!(
                    			(
                					!isQuartz &&
                					block == Blocks.OBSIDIAN
            					) ||
                				(
	                				isQuartz && 
	                				(
	            						block == Blocks.QUARTZ_BLOCK || 
	            						block == Blocks.QUARTZ_STAIRS || 
	            						block == Blocks.GLOWSTONE || 
	            						(
	        								(
	    										block == Blocks.STONE_SLAB2 || 
	    										block == Blocks.STONE_SLAB || 
	    										block == Blocks.DOUBLE_STONE_SLAB2 || 
	    										block == Blocks.DOUBLE_STONE_SLAB
											) && 
											(byte) blockState.getBlock().getMetaFromState(blockState) == 7
										)
									)
								)
							)
                		)
                        {
                            break label24;
                        }
                    }
                    else if (i == this.width - 1)
                    {
                    	IBlockState blockState = this.world.getBlockState(blockpos.offset(this.rightDir));
                        block = blockState.getBlock();

                        if (
                    		!(
                    			(
                					!isQuartz &&
                					block == Blocks.OBSIDIAN
            					) ||                    			
                				(
	                				isQuartz && 
	                				(
	            						block == Blocks.QUARTZ_BLOCK || 
	            						block == Blocks.QUARTZ_STAIRS || 
	            						block == Blocks.GLOWSTONE || 
	            						(
	        								(
	    										block == Blocks.STONE_SLAB2 || 
	    										block == Blocks.STONE_SLAB || 
	    										block == Blocks.DOUBLE_STONE_SLAB2 ||
	    										block == Blocks.DOUBLE_STONE_SLAB
											) && 
											(byte) blockState.getBlock().getMetaFromState(blockState) == 7
										)
									)
								)
							)
						)
                        {
                            break label24;
                        }
                    }
                }
            }

            for (int j = 0; j < this.width; ++j)
            {
            	IBlockState blockState = this.world.getBlockState(this.bottomLeft.offset(this.rightDir, j).up(this.height));
            	Block block = blockState.getBlock();                	
                if (
            		!(
        				(
    						!isQuartz &&
    						block == Blocks.OBSIDIAN
						) ||
        				(
	        				isQuartz && 
	        				(
	    						block == Blocks.QUARTZ_BLOCK || 
	    						block == Blocks.QUARTZ_STAIRS || 
	    						block == Blocks.GLOWSTONE || 
	    						(
									(
										block == Blocks.STONE_SLAB2 ||
										block == Blocks.STONE_SLAB || 
										block == Blocks.DOUBLE_STONE_SLAB2 || 
										block == Blocks.DOUBLE_STONE_SLAB
									) && 
									(byte) blockState.getBlock().getMetaFromState(blockState) == 7
								)
							)
						)
					)
				)
                {
                    this.height = 0;
                    break;
                }
            }

            if (this.height <= 21 && this.height >= 3)
            {
                return this.height;
            }
            else
            {
                this.bottomLeft = null;
                this.width = 0;
                this.height = 0;
                return 0;
            }
        }

        protected boolean isEmptyBlock(Block blockIn)
        {
            return blockIn.getMaterial(null) == Material.AIR || blockIn == Blocks.FIRE || blockIn == Blocks.PORTAL;
        }

        public boolean isValid()
        {
            return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
        }

        public void placePortalBlocks()
        {
            for (int i = 0; i < this.width; ++i)
            {
                BlockPos blockpos = this.bottomLeft.offset(this.rightDir, i);

                for (int j = 0; j < this.height; ++j)
                {
                    this.world.setBlockState(blockpos.up(j), Blocks.PORTAL.getDefaultState().withProperty(BlockPortal.AXIS, this.axis), 2);
                }
            }
        }
    }
    
    public static void placeInExistingPortal(BlockPos pos)
    {    
    	MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
    	WorldServer worldServerInstance = mcServer.worldServerForDimension(0);
    	Long2ObjectMap<Teleporter.PortalPosition> destinationCoordinateCache = null;
    	Teleporter _this = worldServerInstance.getDefaultTeleporter();
		try {
			Field[] fields = _this.getClass().getDeclaredFields();
			for(Field field : fields)
			{
				Class<?> fieldClass = field.getType();
				if(fieldClass.equals(Long2ObjectMap.class))
				{
					field.setAccessible(true);
					destinationCoordinateCache = (Long2ObjectMap) field.get(_this);
			        break;
				}
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        int j = MathHelper.floor_double(pos.getX());
        int k = MathHelper.floor_double(pos.getZ());
        long l = ChunkPos.asLong(j, k);
        
        if (destinationCoordinateCache.containsKey(l))
        {
            Teleporter.PortalPosition teleporter$portalposition = (Teleporter.PortalPosition)destinationCoordinateCache.get(l);
            teleporter$portalposition.lastUpdateTime = worldServerInstance.getTotalWorldTime();
        } else {
        	destinationCoordinateCache.put(l, _this.new PortalPosition(pos, worldServerInstance.getTotalWorldTime()));
        }
    }
}