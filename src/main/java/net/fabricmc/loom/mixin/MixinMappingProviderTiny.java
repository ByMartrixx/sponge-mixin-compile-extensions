/*
 * This file is part of fabric-mixin-compile-extensions, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.mixin;

import net.fabricmc.mappings.*;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.common.MappingProvider;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MixinMappingProviderTiny extends MappingProvider {
	private final String from, to;

	public MixinMappingProviderTiny(Messager messager, Filer filer, String from, String to) {
		super(messager, filer);
		this.from = from;
		this.to = to;
	}

	@Override
	public MappingMethod getMethodMapping(MappingMethod method) {
		MappingMethod mapped = this.methodMap.get(method);
		if (mapped != null)
			return mapped;

		try {
			Class c = this.getClass().getClassLoader().loadClass(method.getOwner().replace('/', '.'));
			if (c == null || c == Object.class) {
				return null;
			}

			for (Class cc : c.getInterfaces()) {
				mapped = getMethodMapping(method.move(cc.getName().replace('.', '/')));
				if (mapped != null) {
					mapped = mapped.move(classMap.getOrDefault(method.getOwner(), method.getOwner()));
					methodMap.put(method, mapped);
					return mapped;
				}
			}

			if (c.getSuperclass() != null) {
				mapped = getMethodMapping(method.move(c.getSuperclass().getName().replace('.', '/')));
				if (mapped != null) {
					mapped = mapped.move(classMap.getOrDefault(method.getOwner(), method.getOwner()));
					methodMap.put(method, mapped);
					return mapped;
				}
			}

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public MappingField getFieldMapping(MappingField field) {
		MappingField mapped = this.fieldMap.get(field);
		if (mapped != null)
			return mapped;

		return null;

		/* try {
			Class c = this.getClass().getClassLoader().loadClass(field.getOwner().replace('/', '.'));
			if (c == null || c == Object.class) {
				return null;
			}

			if (c.getSuperclass() != null) {
				mapped = getFieldMapping(field.move(c.getSuperclass().getName().replace('.', '/')));
				if (mapped != null)
					return mapped;
			}

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} */
	}

	@Override
	public void read(File input) throws IOException {
		Mappings mappings;

		try (FileInputStream stream = new FileInputStream(input)) {
			mappings = MappingsProvider.readTinyMappings(stream, false);
		}

		for (ClassEntry entry : mappings.getClassEntries()) {
			classMap.put(entry.get(from), entry.get(to));
		}

		for (FieldEntry entry : mappings.getFieldEntries()) {
			EntryTriple fromEntry = entry.get(from);
			EntryTriple toEntry = entry.get(to);

			fieldMap.put(
					new MappingField(fromEntry.getOwner(), fromEntry.getName(), fromEntry.getDesc()),
					new MappingField(toEntry.getOwner(), toEntry.getName(), toEntry.getDesc())
			);
		}

		for (MethodEntry entry : mappings.getMethodEntries()) {
			EntryTriple fromEntry = entry.get(from);
			EntryTriple toEntry = entry.get(to);

			methodMap.put(
					new MappingMethod(fromEntry.getOwner(), fromEntry.getName(), fromEntry.getDesc()),
					new MappingMethod(toEntry.getOwner(), toEntry.getName(), toEntry.getDesc())
			);
		}
	}
}
