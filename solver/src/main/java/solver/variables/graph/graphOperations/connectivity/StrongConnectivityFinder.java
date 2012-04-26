/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.variables.graph.graphOperations.connectivity;

import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.BitSet;
import solver.variables.graph.GraphType;
import solver.variables.graph.IActiveNodes;
import solver.variables.graph.INeighbors;
import solver.variables.graph.directedGraph.DirectedGraph;
import solver.variables.graph.directedGraph.IDirectedGraph;

public class StrongConnectivityFinder {

	// input
	private IDirectedGraph graph;
	private BitSet restriction;
	private int n;
	// output
	private int[] sccFirstNode,nextNode, nodeSCC;
	private int nbSCC;

	// util
	INeighbors[] successors;
	int[] stack,p,inf,nodeOfDfsNum,dfsNumOfNode;
	BitSet inStack;

	public StrongConnectivityFinder(IDirectedGraph graph){
		this.graph = graph;
		this.n = graph.getNbNodes();
		//
		stack = new int[n];
		p = new int[n];
		inf = new int[n];
		nodeOfDfsNum = new int[n];
		dfsNumOfNode = new int[n];
		inStack = new BitSet(n);
		successors = new INeighbors[n];
		restriction = new BitSet(n);
		sccFirstNode = new int[n];
		nextNode = new int[n];
		nodeSCC = new int[n];
		nbSCC = 0;
	}

	public void findAllSCC(){
		IActiveNodes nodes = graph.getActiveNodes();
		for(int i=0;i<n;i++){
			restriction.set(i,nodes.isActive(i));
		}
		findAllSCCOf(restriction);
	}

	public void findAllSCCOf(BitSet restriction){
		inStack.clear();
		for(int i=0;i<n;i++){
			dfsNumOfNode[i] = 0;
			inf[i] = n+2;
			nextNode[i] = -1;
			sccFirstNode[i] = -1;
			nodeSCC[i] = -1;
		}
		nbSCC = 0;
		findSingletons(restriction);
		int first = restriction.nextSetBit(0);
		while(first>=0){
			findSCC(first, restriction,
					stack, p, inf, nodeOfDfsNum, dfsNumOfNode, inStack);
			first = restriction.nextSetBit(first);
		}
	}

	private void findSingletons(BitSet restriction){
		IActiveNodes nodes = graph.getActiveNodes();
		for(int i=restriction.nextSetBit(0);i>=0;i=restriction.nextSetBit(i+1)){
			if(nodes.isActive(i) && graph.getPredecessorsOf(i).neighborhoodSize()*graph.getSuccessorsOf(i).neighborhoodSize()==0){
				nodeSCC[i] = nbSCC;
				sccFirstNode[nbSCC++] = i;
				restriction.clear(i);
			}
		}
	}

	private void findSCC(int start, BitSet restriction,int[] stack, int[] p, int[] inf, int[] nodeOfDfsNum, int[] dfsNumOfNode, BitSet inStack){
		int nb = restriction.cardinality();
		// trivial case
		if (nb==1){
			nodeSCC[start] = nbSCC;
			sccFirstNode[nbSCC++] = start;
			restriction.clear(start);
			return;
		}
		//initialization
		int stackIdx= 0;
		int k = 0;
		int i = k;
		dfsNumOfNode[start]=k;
		nodeOfDfsNum[k] = start;
		stack[stackIdx++] = i;
		inStack.set(i);
		p[k] = k;
		successors[k] = graph.getSuccessorsOf(start);
		int j;
		// algo
		boolean notFinished = true;
		boolean first = true;
		while(notFinished){
			if(first){
				j = successors[i].getFirstElement();
			}else{
				j = successors[i].getNextElement();
			}
			first = false;
			if(j>=0){
				if(restriction.get(j)){
					if (dfsNumOfNode[j]==0 && j!=start) {
						k++;
						nodeOfDfsNum[k] = j;
						dfsNumOfNode[j] = k;
						p[k] = i;
						i = k;
						first = true;
						successors[i] = graph.getSuccessorsOf(j);
						stack[stackIdx++] = i;
						inStack.set(i);
						inf[i] = i;
					}else if(inStack.get(dfsNumOfNode[j])){
						inf[i] = Math.min(inf[i], dfsNumOfNode[j]);
					}
				}
			}else{
				if(i==0){
					notFinished = false;
					break;
				}
				if (inf[i] >= i){
					int y,z;
					do{
						z = stack[--stackIdx];
						inStack.clear(z);
						y = nodeOfDfsNum[z];
						restriction.clear(y);
						sccAdd(y);
					}while(z!= i);
					nbSCC++;
				}
				inf[p[i]] = Math.min(inf[p[i]], inf[i]);
				i = p[i];
			}
		}
		if (inStack.cardinality()>0){
			int y;
			do{
				y = nodeOfDfsNum[stack[--stackIdx]];
				restriction.clear(y);
				sccAdd(y);
			}while(y!= start);
			nbSCC++;
		}
	}

	private void sccAdd(int y) {
		nodeSCC[y] = nbSCC;
		nextNode[y] = sccFirstNode[nbSCC];
		sccFirstNode[nbSCC] = y;
	}

	public int getNbSCC() {
		return nbSCC;
	}

	public int[] getNodesSCC() {
		return nodeSCC;
	}

	public int getSCCFirstNode(int i) {
		return sccFirstNode[i];
	}

	public int getNextNode(int j) {
		return nextNode[j];
	}

	// STATIC CODE (to remove?)
	
	/**find all strongly connected components of the partial subgraph of graph 
	 * uses Tarjan algorithm 
	 * run in O(n+m) worst case time
	 * @param graph the input graph
	 * @return all strongly connected components of the partial subgraph of graph
	 */
	public static ArrayList<TIntArrayList> findAllSCCOf(IDirectedGraph graph){
		int n = graph.getNbNodes();
		BitSet bitSCC = new BitSet(n);
		for (int i = graph.getActiveNodes().getFirstElement(); i>=0; i = graph.getActiveNodes().getNextElement()) {
			bitSCC.set(i);
		}
		return findAllSCCOf(graph, bitSCC);
	}

	/**find all strongly connected components of the partial subgraph of graph defined by restriction
	 * uses Tarjan algorithm 
	 * run in O(n+m) worst case time
	 * @param graph the input graph
	 * @param restriction provide a restriction : nodes set to 0 (false) will be ignored
	 * @return all strongly connected components of the partial subgraph of graph defined by restriction
	 */
	public static ArrayList<TIntArrayList> findAllSCCOf(IDirectedGraph graph, BitSet restriction){
		int nb = restriction.cardinality();
		int[] stack = new int[nb];
		int[] p = new int[nb];
		int[] inf = new int[nb];
		int[] nodeOfDfsNum = new int[nb];
		int[] dfsNumOfNode = new int[graph.getNbNodes()];
		BitSet inStack = new BitSet(nb);
		ArrayList<TIntArrayList> allSCC = new ArrayList<TIntArrayList>(nb);
		findSingletons(graph, restriction, allSCC);
		int first = restriction.nextSetBit(0);
		while(first>=0){
			findSCC(allSCC, graph, first, restriction,
					stack, p, inf, nodeOfDfsNum, dfsNumOfNode, inStack);
			first = restriction.nextSetBit(first);
		}
		return allSCC;
	}

	/**Find singletons
	 * run in O(n)
	 * @param graph
	 * @param iterable
	 * @param allSCC
	 */
	private static void findSingletons(IDirectedGraph graph, BitSet iterable, ArrayList<TIntArrayList> allSCC){
		for(int i=iterable.nextSetBit(0); i>=0; i=iterable.nextSetBit(i+1)){
			if(graph.getPredecessorsOf(i).neighborhoodSize()*graph.getSuccessorsOf(i).neighborhoodSize()==0){
				TIntArrayList scc = new TIntArrayList();
				scc.add(i);
				allSCC.add(scc);
				iterable.clear(i);
			}
		}
	}

	/** find all strongly connected components of the partial subgraph of graph defined by restriction reachable from the node start, and add them to the set allSCC
	 * uses Tarjan algorithm 
	 * run in O(n+m) worst case time
	 *
	 * @param allSCC the set of all scc of graph
	 * @param graph the input graph
	 * @param start the node to start the search at
	 * @param restriction provide a restriction : nodes set to 0 (false) will be ignored
	 * @param stack
	 * @param p
	 * @param inf
	 * @param nodeOfDfsNum
	 * @param dfsNumOfNode
	 * @param inStack
	 */
	private static void findSCC(ArrayList<TIntArrayList> allSCC, IDirectedGraph graph, int start, BitSet restriction,int[] stack, int[] p, int[] inf, int[] nodeOfDfsNum, int[] dfsNumOfNode, BitSet inStack){
		int nb = restriction.cardinality();
		// trivial case
		if (nb==1){
			TIntArrayList scc = new TIntArrayList();
			scc.add(start);
			allSCC.add(scc);
			restriction.clear(start);
			return;
		}
		//initialization
		int stackIdx= 0;
		INeighbors[] successors = new INeighbors[nb];
		for(int m=0;m<nb;m++){
			inf[m] = nb+2;
		}
		int k = 0;
		int i = k;
		dfsNumOfNode[start]=k;
		nodeOfDfsNum[k] = start;
		stack[stackIdx++] = i;
		inStack.set(i);
		p[k] = k;
		successors[k] = graph.getSuccessorsOf(start);
		int j = 0;
		// algo
		boolean notFinished = true;
		boolean first = true;
		while(notFinished){
			if(first){
				j = successors[i].getFirstElement();
			}else{
				j = successors[i].getNextElement();
			}
			first = false;
			if(j>=0){
				if(restriction.get(j)){
					if (dfsNumOfNode[j]==0 && j!=start) {
						k++;
						nodeOfDfsNum[k] = j;
						dfsNumOfNode[j] = k;
						p[k] = i;
						i = k;
						first = true;
						successors[i] = graph.getSuccessorsOf(j);
						stack[stackIdx++] = i;
						inStack.set(i);
						inf[i] = i;
					}else if(inStack.get(dfsNumOfNode[j])){
						inf[i] = Math.min(inf[i], dfsNumOfNode[j]);
					}
				}
			}else{
				if(i==0){
					notFinished = false;
					break;
				}
				if (inf[i] >= i){
					TIntArrayList scc = new TIntArrayList();
					int y,z;
					do{
						z = stack[--stackIdx];
						inStack.clear(z);
						y = nodeOfDfsNum[z];
						restriction.clear(y);
						scc.add(y);
					}while(z!= i);
					allSCC.add(scc);
				}
				inf[p[i]] = Math.min(inf[p[i]], inf[i]);
				i = p[i];
			}
		}
		if (inStack.cardinality()>0){
			TIntArrayList scc = new TIntArrayList();
			int y;
			do{
				y = nodeOfDfsNum[stack[--stackIdx]];
				restriction.clear(y);
				scc.add(y);
			}while(y!= start);
			allSCC.add(scc);
		}
	}
}
