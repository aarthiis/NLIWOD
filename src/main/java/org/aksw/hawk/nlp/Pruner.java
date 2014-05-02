package org.aksw.hawk.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.aksw.autosparql.commons.qald.Question;
import org.aksw.hawk.module.Module;
import org.aksw.hawk.nlp.posTree.MutableTree;
import org.aksw.hawk.nlp.posTree.MutableTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pruner {
	Logger log = LoggerFactory.getLogger(Pruner.class);

	public MutableTree prune(Question q) {
		log.info(q.tree.toString());
		applyPunctuationRules(q);
		applyDeterminantRules(q);
		applyPDTRules(q);
		applyINRules(q);
		applyAuxPassRules(q);
		applyJJRule(q);// TODO JJ rule is very vague
		// applyCombineNNRule(q);
		// applyWDTRule(q);
		/*
		 * interrogative rules last else each interrogative word has at least
		 * two children, which can't be handled yet by the removal
		 */
		applyInterrogativeRules(q);
		sortTree(q.tree);
		log.info(q.tree.toString());
		return q.tree;
	}

	/**
	 * This rule is used to combine nouns e.g. >did (0|VBD) |=>many
	 * awards(1|NNS) |==>How (3|WRB) |==>Globe (4|NNP) |===>Golden (5|NNP)
	 * |=>husband (6|NN) |==>http://dbpedia.org/resource/Katie_Holmes (9|ADD)
	 * |=>win (10|VB) >
	 * 
	 * should be combined to Golden Globe (NNP)
	 * 
	 * @param q
	 */
	private void applyCombineNNRule(Question q) {
		List<Module> tmp = new ArrayList<>();
		// traverse sub arguments depth first search
		Stack<MutableTreeNode> stack = new Stack<>();
		stack.push(q.tree.getRoot());
		// if NN(*) is found search in the ascending nodes a NN, NNS, NNP or
		// NNPS
		while (!stack.isEmpty()) {
			MutableTreeNode pop = stack.pop();
			if (pop.posTag.matches("NN(.)*") && pop.parent != null) {
				MutableTreeNode parent = pop.parent;
				while (parent != null) {
					if (parent.posTag.matches("NN(.)*")) {
						break;
					}
					parent = parent.parent;
				}
				if (parent != null) {
					parent.label = pop.label + " " + parent.label;
				}
				q.tree.remove(pop);
			}
			List<MutableTreeNode> children = pop.getChildren();
			Collections.reverse(children);
			for (MutableTreeNode child : children) {
				stack.push(child);
			}
		}

	}

	private void applyWDTRule(Question q) {
		inorderRemovalWDTRule(q.tree.getRoot(), q.tree);

	}

	private boolean inorderRemovalWDTRule(MutableTreeNode node, MutableTree tree) {
		if (node.posTag.equals("WDT")) {
			tree.remove(node);
			return true;
		} else {
			for (Iterator<MutableTreeNode> it = node.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode child = it.next();
				if (inorderRemovalWDTRule(child, tree)) {
					it = node.getChildren().iterator();
				}
			}
			return false;
		}
	}

	// TODO if no NN above JJ, the JJ node is delete anyways, correct bahaviour?
	private void applyJJRule(Question q) {
		// delete JJ node and paste JJ content to NN or NNS or NNP or NNPS
		// to pretend anti-apartheid activist gets splitted
		List<Module> tmp = new ArrayList<>();
		// traverse sub arguments depth first search
		Stack<MutableTreeNode> stack = new Stack<>();
		stack.push(q.tree.getRoot());
		while (!stack.isEmpty()) {
			MutableTreeNode pop = stack.pop();
			// if JJ is found search in the ascending nodes a NN, NNS, NNP or
			// NNPS
			if (pop.posTag.equals("JJ") && pop.parent != null) {
				MutableTreeNode parent = pop.parent;
				while (parent != null) {
					if (parent.posTag.equals("NN") || parent.posTag.equals("NNS") || parent.posTag.equals("NNP") || parent.posTag.equals("NNPS")) {
						break;
					}
					parent = parent.parent;
				}
				if (parent != null) {
					parent.label = pop.label + " " + parent.label;
				}
				q.tree.remove(pop);
			}
			List<MutableTreeNode> children = pop.getChildren();
			Collections.reverse(children);
			for (MutableTreeNode child : children) {
				stack.push(child);
			}
		}

	}

	private void sortTree(MutableTree tree) {
		Queue<MutableTreeNode> queue = new LinkedList<MutableTreeNode>();
		queue.add(tree.getRoot());
		while (!queue.isEmpty()) {
			MutableTreeNode tmp = queue.poll();
			Collections.sort(tmp.getChildren());
			queue.addAll(tmp.getChildren());
		}

	}

	private void applyAuxPassRules(Question q) {
		inorderRemovalAuxPass(q.tree.getRoot(), q.tree);

	}

	private boolean inorderRemovalAuxPass(MutableTreeNode node, MutableTree tree) {
		if (node.depLabel.equals("auxpass")) {
			tree.remove(node);
			return true;
		} else {
			for (Iterator<MutableTreeNode> it = node.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode child = it.next();
				if (inorderRemovalAuxPass(child, tree)) {
					it = node.getChildren().iterator();
				}
			}
			return false;
		}
	}

	private void applyPDTRules(Question q) {
		inorderRemovalPDT(q.tree.getRoot(), q.tree);

	}

	private boolean inorderRemovalPDT(MutableTreeNode node, MutableTree tree) {
		if (node.posTag.equals("PDT")) {
			tree.remove(node);
			return true;
		} else {
			for (Iterator<MutableTreeNode> it = node.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode child = it.next();
				if (inorderRemovalPDT(child, tree)) {
					it = node.getChildren().iterator();
				}
			}
			return false;
		}
	}

	// removes BY and IN
	private void applyINRules(Question q) {
		inorderRemovalIN(q.tree.getRoot(), q.tree);

	}

	private boolean inorderRemovalIN(MutableTreeNode node, MutableTree tree) {
		if (node.posTag.equals("IN")) {
			tree.remove(node);
			return true;
		} else {
			for (Iterator<MutableTreeNode> it = node.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode child = it.next();
				if (inorderRemovalIN(child, tree)) {
					it = node.getChildren().iterator();
				}
			}
			return false;
		}
	}

	private void applyInterrogativeRules(Question q) {
		MutableTreeNode root = q.tree.getRoot();
		// GIVE ME will be deleted
		if (root.label.equals("Give")) {
			for (Iterator<MutableTreeNode> it = root.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode next = it.next();
				if (next.label.equals("me")) {
					it.remove();
					q.tree.remove(root);
				}
			}
		}
		// LIST
		if (root.label.equals("List")) {
			q.tree.remove(root);
		}

	}

	private void applyDeterminantRules(Question q) {
		inorderRemovalDeterminats(q.tree.getRoot(), q.tree);

	}

	private boolean inorderRemovalDeterminats(MutableTreeNode node, MutableTree tree) {
		if (node.posTag.equals("DT")) {
			tree.remove(node);
			return true;
		} else {
			for (Iterator<MutableTreeNode> it = node.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode child = it.next();
				if (inorderRemovalDeterminats(child, tree)) {
					it = node.getChildren().iterator();
				}
			}
			return false;
		}
	}

	private void applyPunctuationRules(Question q) {
		inorderRemovalPunctuations(q.tree.getRoot(), q.tree);

	}

	private boolean inorderRemovalPunctuations(MutableTreeNode node, MutableTree tree) {
		if (node.posTag.equals(".")) {
			tree.remove(node);
			return true;
		} else {
			for (Iterator<MutableTreeNode> it = node.getChildren().iterator(); it.hasNext();) {
				MutableTreeNode child = it.next();
				if (inorderRemovalPunctuations(child, tree)) {
					it = node.getChildren().iterator();
				}
			}
			return false;
		}

	}

}
