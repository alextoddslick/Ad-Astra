/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.api.client.model.loading.v1;

import java.util.function.BiFunction;
import net.minecraft.class_10419;
import net.minecraft.class_10801;
import net.minecraft.class_10819;
import net.minecraft.class_1086;
import net.minecraft.class_1087;
import net.minecraft.class_10893;
import net.minecraft.class_2960;
import net.minecraft.class_3665;
import net.minecraft.class_7775;

/**
 * A {@link UnbakedExtraModel} that loads a single model.
 *
 * @param <T> The type of the baked model, for instance {@link class_1087}.
 */
public final class SimpleUnbakedExtraModel<T> implements UnbakedExtraModel<T> {
	private final class_2960 model;
	private final BiFunction<class_10819, class_7775, T> bake;

	/**
	 * @param model The location of the model to load.
	 * @param bake  A function to bake the model.
	 */
	public SimpleUnbakedExtraModel(class_2960 model, BiFunction<class_10819, class_7775, T> bake) {
		this.model = model;
		this.bake = bake;
	}

	/**
	 * Create a {@link SimpleUnbakedExtraModel} for a {@link class_1087}.
	 *
	 * <h2>Example</h2>
	 * {@snippet :
	 * public static final Identifier MODEL_ID = Identifier.fromNamespaceAndPath("modid", "model_path");
	 * public static final ExtraModelKey<BlockStateModel> MODEL_KEY = ExtraModelKey.create(MODEL_ID::toString);
	 *
	 * public static void register() {
	 * 		ModelLoadingPlugin.register(pluginContext -> pluginContext.addModel(MODEL_KEY, SimpleUnbakedExtraModel.blockStateModel(MODEL_ID)));
	 * }
	 * }
	 *
	 * @param model The location of the model to load.
	 * @return The unbaked extra model.
	 */
	public static SimpleUnbakedExtraModel<class_1087> blockStateModel(class_2960 model) {
		return blockStateModel(model, class_1086.field_63619);
	}

	/**
	 * Create a {@link SimpleUnbakedExtraModel} for a {@link class_1087}.
	 *
	 * @param model    The location of the model to load.
	 * @param settings The settings to bake the geometry with.
	 * @return The unbaked extra model.
	 */
	public static SimpleUnbakedExtraModel<class_1087> blockStateModel(class_2960 model, class_3665 settings) {
		return new SimpleUnbakedExtraModel<>(model, (baked, baker) -> {
			class_10419 textures = baked.method_68045();
			return new class_10893(new class_10801(
					baked.method_68034(textures, baker, settings),
					baked.method_68040(),
					baked.method_68033(textures, baker)
			));
		});
	}

	@Override
	public void method_62326(class_10103 resolver) {
		resolver.markDependency(model);
	}

	@Override
	public T bake(class_7775 baker) {
		return bake.apply(baker.method_45872(model), baker);
	}
}
