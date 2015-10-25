/* 
 * Copyright (C) 2007  Rajarshi Guha <rajarshi@users.sourceforge.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.smiles.smarts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.knime.core.data.def.IntCell;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.ComponentGrouping;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.SmartsStereoMatch;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;

public class SmartSMARTSQueryTool {

	private final LinkedHashMap<Pattern, QueryAtomContainer> queries;

	private static final boolean RING_QUERY = true;

	public SmartSMARTSQueryTool(List<String> smarts) {

		this.queries = new LinkedHashMap<Pattern, QueryAtomContainer>();
		for (String smart : smarts) {
			QueryAtomContainer query = SMARTSParser.parse(smart, SilentChemObjectBuilder.getInstance());
			this.queries.put(VentoFoggia.findSubstructure(query), query);
		}
	}

	public boolean matches(IAtomContainer atomContainer) throws CDKException {

		SmartsMatchers.prepare(atomContainer, RING_QUERY);

		for (Entry<Pattern, QueryAtomContainer> entry : queries.entrySet()) {
			Mappings mappings = entry.getKey().matchAll(atomContainer)
					.filter(new SmartsStereoMatch(entry.getValue(), atomContainer))
					.filter(new ComponentGrouping(entry.getValue(), atomContainer));
			
			if (mappings.atLeast(1)) {
				return true;
			}
		}

		return false;
	}
	
	public List<IntCell> countUnique(IAtomContainer atomContainer) throws CDKException {
		
		SmartsMatchers.prepare(atomContainer, RING_QUERY);

		List<IntCell> total = new ArrayList<>();
		for (Entry<Pattern, QueryAtomContainer> entry : queries.entrySet()) {
			Mappings mappings = entry.getKey().matchAll(atomContainer)
					.filter(new SmartsStereoMatch(entry.getValue(), atomContainer))
					.filter(new ComponentGrouping(entry.getValue(), atomContainer));
			
			total.add(new IntCell(mappings.countUnique()));
		}
		
		return total;
	}
}
