package me.neonjava.neonorder.obj;

import io.papermc.paper.math.BlockPosition;
import org.bukkit.block.BlockType;

import java.util.function.Consumer;

public record SignInfo(Consumer<String> action, BlockType signBlock, BlockPosition pos) {
}
