/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpData;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.UAElementFactory;
import org.eclipse.help.internal.util.ProductPreferences;

/*
 * Manages toc contributions (TocContribution) supplied by the various toc
 * providers (AbstractTocProvider).
 */
public class TocManager {
	
	private static final String EXTENSION_POINT_ID_TOC = HelpPlugin.PLUGIN_ID + ".toc"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_TOC_PROVIDER = "tocProvider"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$
	
	private AbstractTocProvider[] tocProviders;
	private Map tocContributionsByLocale = new HashMap();
	private Map tocsByLocale = new HashMap();
	private Map tocsById = new HashMap();
	private Map tocsByTopic;
	
	/*
	 * Returns all toc entries (complete books) for the given locale.
	 */
	public synchronized Toc[] getTocs(String locale) {
		Toc[] tocs = (Toc[])tocsByLocale.get(locale);
		if (tocs == null) {
			TocContribution[] raw = getRootTocContributions(locale);
			TocContribution[] filtered = filterTocContributions(raw);
			TocContribution[] ordered = orderTocContributions(filtered);
			List orderedTocs = new ArrayList(ordered.length);
			for (int i=0;i<ordered.length;++i) {
				try {
					Toc toc = (Toc)ordered[i].getToc();
					orderedTocs.add(toc);
					tocsById.put(ordered[i].getId(), toc);
				}
				catch (Throwable t) {
					// log and skip
					String msg = "Error getting " + Toc.class.getName() + " from " + ITocContribution.class.getName() + ": " + ordered[i]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					HelpPlugin.logError(msg, t);
				}
			}
			tocs = (Toc[])orderedTocs.toArray(new Toc[orderedTocs.size()]);
			tocsByLocale.put(locale, tocs);
		}
		return tocs;
	}
	
	/*
	 * Returns the toc whose toc contribution has the given id, for the
	 * given locale.
	 */
	public synchronized Toc getToc(String id, String locale) {
		getTocs(locale);
		return (Toc)tocsById.get(id);
	}
	
	public synchronized Toc getOwningToc(String href) {
		if (tocsByTopic == null) {
			tocsByTopic = new HashMap();
			Toc[] tocs = HelpPlugin.getTocManager().getTocs(Platform.getNL());
			for (int i=0;i<tocs.length;++i) {
				ITocContribution contribution = tocs[i].getTocContribution();
				String[] extraDocuments = contribution.getExtraDocuments();
				for (int j=0;j<extraDocuments.length;++j) {
					tocsByTopic.put(extraDocuments[j], tocs[i]);
				}
			}
		}
		return (Toc)tocsByTopic.get(href);
	}
	
	public synchronized Topic getTopic(String href) {
		Toc[] tocs = HelpPlugin.getTocManager().getTocs(Platform.getNL());
		for (int i=0;i<tocs.length;++i) {
			Topic topic = (Topic)tocs[i].getTopic(href);
			if (topic != null) {
				return topic;
			}
		}
		int index = href.indexOf('#');
		if (index != -1) {
			return getTopic(href.substring(0, index));
		}
		return null;
	}
	
	public synchronized int[] getTopicPath(String href) {
		Topic topic = getTopic(href);
		if (topic != null) {
			List path = new ArrayList();
			UAElement element = topic;
			while (!(element instanceof Toc)) {
				UAElement parent = element.getParentElement();
				path.add(new Integer(indexOf(parent, (Topic)element)));
				element = parent;
			}
			Toc[] tocs = getTocs(Platform.getNL());
			for (int i=0;i<tocs.length;++i) {
				if (tocs[i] == element) {
					path.add(new Integer(i));
					int[] array = new int[path.size()];
					for (int j=0;j<array.length;++j) {
						array[j] = ((Integer)path.get(array.length - 1 - j)).intValue();
					}
					return array;
				}
			}
		}
		// no path; not in toc
		return null;
	}
	
	/*
	 * Returns the zero-based index at which the child topic is located under
	 * the parent topic/toc.
	 */
	private int indexOf(UAElement parent, Topic child) {
		ITopic[] children;
		if (parent instanceof Topic) {
			children = ((Topic)parent).getSubtopics();
		}
		else if (parent instanceof Toc) {
			children = ((Toc)parent).getTopics();
		}
		else {
			return -1;
		}
		for (int i=0;i<children.length;++i) {
			if (children[i] == child) {
				return i;
			}
		}
		return -1;
	}
	
	/*
	 * Returns all toc contributions for the given locale, from all toc
	 * providers.
	 */
	public synchronized TocContribution[] getTocContributions(String locale) {
		TocContribution[] cached = (TocContribution[])tocContributionsByLocale.get(locale);
		if (cached == null) {
			List contributions = new ArrayList();
			AbstractTocProvider[] providers = getTocProviders();
			for (int i=0;i<providers.length;++i) {
				ITocContribution[] contrib;
				try {
					contrib = providers[i].getTocContributions(locale);
				}
				catch (Throwable t) {
					// log, and skip the offending provider
					String msg = "Error getting help table of contents data from provider: " + providers[i].getClass().getName() + " (skipping provider)"; //$NON-NLS-1$ //$NON-NLS-2$
					HelpPlugin.logError(msg, t);
					continue;
				}
				
				for (int j=0;j<contrib.length;++j) {
					TocContribution contribution = new TocContribution();
					contribution.setCategoryId(contrib[j].getCategoryId());
					contribution.setContributorId(contrib[j].getContributorId());
					contribution.setExtraDocuments(contrib[j].getExtraDocuments());
					contribution.setId(contrib[j].getId());
					contribution.setLocale(contrib[j].getLocale());
					contribution.setPrimary(contrib[j].isPrimary());
					IToc toc = contrib[j].getToc();
					contribution.setToc(toc instanceof Toc ? (Toc)toc : (Toc)UAElementFactory.newElement(toc));
					contributions.add(contribution);
				}
			}
			cached = (TocContribution[])contributions.toArray(new TocContribution[contributions.size()]);
			tocContributionsByLocale.put(locale, cached);
		}
		return cached;
	}

	/*
	 * Clears all cached contributions, forcing the manager to query the
	 * providers again next time a request is made.
	 */
	public void clearCache() {
		tocContributionsByLocale.clear();
		tocsByLocale.clear();
		tocsById.clear();
		tocsByTopic = null;
	}

	/*
	 * Internal hook for unit testing.
	 */
	public AbstractTocProvider[] getTocProviders() {
		if (tocProviders == null) {
			List providers = new ArrayList();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_TOC);
			for (int i=0;i<elements.length;++i) {
				IConfigurationElement elem = elements[i];
				if (elem.getName().equals(ELEMENT_NAME_TOC_PROVIDER)) {
					try {
						AbstractTocProvider provider = (AbstractTocProvider)elem.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
						providers.add(provider);
					}
					catch (CoreException e) {
						// log and skip
						String msg = "Error instantiating help table of contents provider class \"" + elem.getAttribute(ATTRIBUTE_NAME_CLASS) + '"'; //$NON-NLS-1$
						HelpPlugin.logError(msg, e);
					}
				}
			}
			tocProviders = (AbstractTocProvider[])providers.toArray(new AbstractTocProvider[providers.size()]);
		}
		return tocProviders;
	}

	/*
	 * Internal hook for unit testing.
	 */
	public void setTocProviders(AbstractTocProvider[] tocProviders) {
		this.tocProviders = tocProviders;
	}

	/*
	 * Filters the given contributions according to product preferences. If
	 * either the contribution's id or its category's id is listed in the
	 * ignoredTocs, filter the contribution.
	 */
	private TocContribution[] filterTocContributions(TocContribution[] unfiltered) {
		Set tocsToFilter = getIgnoredTocContributions();
		List filtered = new ArrayList();
		Set ignoredHrefs = new HashSet();
		Set notIgnoredHrefs = new HashSet();
		for (int i=0;i<unfiltered.length;++i) {
			Toc toc = (Toc)unfiltered[i].getToc();
			Set hrefs = toc.getHref2TopicMap().keySet();
			if (!tocsToFilter.contains(unfiltered[i].getId()) &&
					!tocsToFilter.contains(unfiltered[i].getCategoryId())) {
				filtered.add(unfiltered[i]);
				notIgnoredHrefs.addAll(hrefs);
			}
			else {
				ignoredHrefs.addAll(hrefs);
			}
		}
		return (TocContribution[])filtered.toArray(new TocContribution[filtered.size()]);
	}

	private TocContribution[] getRootTocContributions(String locale) {
		TocContribution[] contributions = getTocContributions(locale);
		List unassembled = new ArrayList(Arrays.asList(contributions));
		TocAssembler assembler = new TocAssembler();
		List assembled = assembler.assemble(unassembled);
		return (TocContribution[])assembled.toArray(new TocContribution[assembled.size()]);
	}
	
	private Set getIgnoredTocContributions() {
		HelpData helpData = HelpData.getProductHelpData();
		if (helpData != null) {
			return helpData.getHiddenTocs();
		}
		else {
			HashSet ignored = new HashSet();
			Preferences pref = HelpPlugin.getDefault().getPluginPreferences();
			String preferredTocs = pref.getString(HelpPlugin.IGNORED_TOCS_KEY);
			if (preferredTocs.length() > 0) {
				StringTokenizer suggestdOrderedInfosets = new StringTokenizer(preferredTocs, " ;,"); //$NON-NLS-1$
				while (suggestdOrderedInfosets.hasMoreTokens()) {
					ignored.add(suggestdOrderedInfosets.nextToken());
				}
			}
			return ignored;
		}
	}

	/*
	 * Orders the given toc contributions by category and product preference.
	 */
	private TocContribution[] orderTocContributions(TocContribution[] unorderedTocs) {
		// first categorize the TOCs
		List itemsToOrder = new ArrayList();
		Map categorized = categorizeTocs(Arrays.asList(unorderedTocs), itemsToOrder);
			
		// order them
		List orderedItems = ProductPreferences.getTocOrder(itemsToOrder);
			
		// replace with actual TocContribution or category
		orderedItems = substituteValues(orderedItems, categorized);
			
		// expand the categories
		orderedItems = expandCategories(orderedItems);
		return (TocContribution[])orderedItems.toArray(new TocContribution[orderedItems.size()]);
	}
	
	/*
	 * Categorizes the given toc contributions into categories, or individual
	 * toc contributions if no category (treated as a category of one). Returns
	 * mapping from category id/toc id to category/toc. Order of categories/
	 * tocs is returned via tocOrder.
	 */
	private Map categorizeTocs(List tocs, List tocOrder) {
		Map categorized = new HashMap();
		Iterator iter = tocs.iterator();
		while (iter.hasNext()) {
			ITocContribution toc = (ITocContribution)iter.next();
			String categoryId;
			try {
				categoryId = toc.getCategoryId();
			}
			catch (Throwable t) {
				// log and skip
				String msg = "Error retrieving categoryId from " + ITocContribution.class.getName() + ": " + toc.getClass().getName(); //$NON-NLS-1$ //$NON-NLS-2$
				HelpPlugin.logError(msg, t);
				continue;
			}
			if (categoryId != null) {
				// it has a category, add it to the appropriate TocCategory
				TocCategory category = (TocCategory)categorized.get(categoryId);
				if (category == null) {
					// create categories as needed
					category = new TocCategory(categoryId);
					categorized.put(categoryId, category);
					tocOrder.add(categoryId);
				}
				category.add(toc);
			}
			else {
				// doesn't have a category; insert the TOC directly
				String id;
				try {
					id = toc.getId();
				}
				catch (Throwable t) {
					// log and skip
					String msg = "Error retrieving id from " + ITocContribution.class.getName() + ": " + toc.getClass().getName(); //$NON-NLS-1$ //$NON-NLS-2$
					HelpPlugin.logError(msg, t);
					continue;
				}
				categorized.put(id, toc);
				tocOrder.add(id);
			}
		}
		return categorized;
	}
	
	/*
	 * Expands all categories in the given list to actual toc contributions
	 * organized by category.
	 */
	private List expandCategories(List entries) {
		List expanded = new ArrayList();
		Iterator iter = entries.iterator();
		while (iter.hasNext()) {
			Object entry = iter.next();
			if (entry instanceof ITocContribution) {
				expanded.add(entry);
			}
			else if (entry instanceof TocCategory) {
				expanded.addAll((TocCategory)entry);
			}
		}
		return expanded;
	}

	/*
	 * Returns whether or not the toc for the given locale has been completely
	 * loaded yet or not.
	 */
	public boolean isTocLoaded(String locale) {
		return tocsByLocale.get(locale) != null;
	}
	
	/*
	 * Substitutes each item with it's corresponding mapping from the map.
	 * Original List is not modified.
	 */
	private static List substituteValues(List items, Map map) {
		if (items != null && map != null) {
			List result = new ArrayList(items.size());
			Iterator iter = items.iterator();
			while (iter.hasNext()) {
				Object key = iter.next();
				Object value = map.get(key);
				if (value != null) {
					result.add(value);
				}
			}
			return result;
		}
		return null;
	}
		
	/*
	 * A category of tocs. A category has an id and a list of contained
	 * tocs.
	 */
	private static class TocCategory extends ArrayList {
		
		private static final long serialVersionUID = 1L;
		private String id;
		
		/**
		 * Constructs a new empty TOC category with the given id.
		 * 
		 * @param id the category's id
		 */
		public TocCategory(String id) {
			this.id = id;
		}
		
		/**
		 * Returns the category's id.
		 * 
		 * @return the id of the category
		 */
		public String getId() {
			return id;
		}
	}
}
