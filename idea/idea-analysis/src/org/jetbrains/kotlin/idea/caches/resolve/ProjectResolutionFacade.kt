/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompositeBindingContext

internal class ProjectResolutionFacade(
        val debugString: String,
        val project: Project,
        val globalContext: GlobalContextImpl,
        computeModuleResolverProvider: (GlobalContextImpl, Project) -> ModuleResolverProvider
) {
    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
            {
                val resolverProvider = computeModuleResolverProvider(globalContext, project)
                CachedValueProvider.Result.create(resolverProvider, resolverProvider.cacheDependencies)
            },
            /* trackValue = */ false
    )

    val moduleResolverProvider: ModuleResolverProvider
        get() = globalContext.storageManager.compute { cachedValue.value }

    fun resolverForModuleInfo(moduleInfo: IdeaModuleInfo) = moduleResolverProvider.resolverForProject.resolverForModule(moduleInfo)
    fun resolverForDescriptor(moduleDescriptor: ModuleDescriptor) = moduleResolverProvider.resolverForProject.resolverForModuleDescriptor(moduleDescriptor)

    fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor {
        return moduleResolverProvider.resolverForProject.descriptorForModule(ideaModuleInfo)
    }

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
            {
                val resolverProvider = moduleResolverProvider
                val results = object : SLRUCache<KtFile, PerFileAnalysisCache>(2, 3) {
                    override fun createValue(file: KtFile): PerFileAnalysisCache {
                        return PerFileAnalysisCache(file, resolverProvider.resolverForProject.resolverForModule(file.getModuleInfo()).componentProvider)
                    }
                }

                val allDependencies = resolverProvider.cacheDependencies + listOf(PsiModificationTracker.MODIFICATION_COUNT)
                CachedValueProvider.Result.create(results, allDependencies)
            }, false)

    fun getAnalysisResultsForElements(elements: Collection<KtElement>): AnalysisResult {
        assert(elements.isNotEmpty()) { "elements collection should not be empty" }
        val slruCache = synchronized(analysisResults) {
            analysisResults.value!!
        }
        val results = elements.map {
            val perFileCache = synchronized(slruCache) {
                slruCache[it.getContainingKtFile()]
            }
            perFileCache.getAnalysisResults(it)
        }
        val withError = results.firstOrNull { it.isError() }
        val bindingContext = CompositeBindingContext.create(results.map { it.bindingContext })
        return if (withError != null)
            AnalysisResult.error(bindingContext, withError.error)
        else
        //TODO: (module refactoring) several elements are passed here in debugger
            AnalysisResult.success(bindingContext, findModuleDescriptor(elements.first().getModuleInfo()))
    }

    override fun toString(): String {
        return "$debugString@${Integer.toHexString(hashCode())}"
    }
}