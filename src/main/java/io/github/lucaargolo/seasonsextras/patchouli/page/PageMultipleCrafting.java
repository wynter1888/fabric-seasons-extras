package io.github.lucaargolo.seasonsextras.patchouli.page;

import com.google.gson.annotations.SerializedName;
import io.github.lucaargolo.seasonsextras.utils.Tickable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.Identifier;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.page.PageCrafting;

public class PageMultipleCrafting extends PageCrafting implements Tickable {

    @SerializedName("recipes") Identifier[] recipes;
    private transient Recipe<?>[] loadedRecipes;

    private transient int age = 0;

    @Override
    public void build(BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(entry, builder, pageNum);
        loadedRecipes = new Recipe[recipes.length];
        for(int i = 0; i < recipes.length; i++) {
            loadedRecipes[i] = loadRecipe(builder, entry, recipes[i]);
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float pticks) {
        if(loadedRecipes != null && loadedRecipes.length > 0) {
            int index = (age/20) % loadedRecipes.length;
            recipe1 = loadedRecipes[index];
        }
        super.render(ms, mouseX, mouseY, pticks);
    }

    @Override
    public void tick() {
        age++;
    }
}
