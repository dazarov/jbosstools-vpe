/*******************************************************************************
 * Copyright (c) 2007-2009 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.editor.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IFile;
import org.jboss.tools.vpe.editor.VpeIncludeInfo;
import org.jboss.tools.vpe.editor.VpeVisualDomBuilder;
import org.jboss.tools.vpe.editor.context.VpePageContext;
import org.jboss.tools.vpe.editor.mapping.VpeElementMapping;
import org.jboss.tools.vpe.editor.util.HTML;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDOMElement;
import org.mozilla.interfaces.nsIDOMNamedNodeMap;
import org.mozilla.interfaces.nsIDOMNode;
import org.mozilla.interfaces.nsIDOMNodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class VpeDefineContainerTemplate extends VpeAbstractTemplate {
    private static Set<Node> defineContainer = new HashSet<Node>();
    private static final Pattern CURLY_BRACKET_PATTERN = 
    		Pattern.compile("(" + Pattern.quote("#") + "\\{(.+?)\\})+?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final Pattern WORD_PATTERN = Pattern.compile("((\\w+)([\\.\\[\\]]*))");  //$NON-NLS-1$
    
    @Override
    protected void init(Element templateElement) {
	children = true;
	modify = false;
	initTemplateSections(templateElement, false, true, false, false, false);
    }

    public VpeCreationData createTemplate(String includedFileName,
    		VpePageContext pageContext, Node sourceNode,
    		nsIDOMDocument visualDocument) {
    	VpeCreationData creationData = null;
    	if (includedFileName != null && includedFileName.trim().length() > 0) {
    		IFile file = VpeCreatorUtil.getFile(includedFileName, pageContext);
    		if ((file != null) && !pageContext.getVisualBuilder().isFileInIncludeStack(file)) {
    			Document document = pageContext.getVisualBuilder()
    					.getIncludeDocuments().get(file);
    			if (document == null) {
    				document = VpeCreatorUtil.getDocumentForRead(file);
    			}
    			// Document document = VpeCreatorUtil.getDocumentForRead(file, pageContext);
    			if (document != null) {
    				/*
    				 * Putting include document
    				 */
    				pageContext.getVisualBuilder()
    					.getIncludeDocuments().put(file, document);
    				creationData = createInclude(document, visualDocument);
    				creationData.setData(new TemplateFileInfo(file));
    				/*
    				 * Pushing include stack and right after that --
    				 * registering <UI:DEFINE> element.
    				 */
    				pageContext.getVisualBuilder().pushIncludeStack(
    						new VpeIncludeInfo((Element) sourceNode, file, document));
    				registerDefine(pageContext, sourceNode);
    				/*
    				 * we should add only real node, sourceNode can be a proxy,
    				 * so there is a trick to get the node:
    				 * get the parent of the first child. 
    				 */
    				if (sourceNode.getFirstChild() != null) {
    					defineContainer.add(sourceNode.getFirstChild().getParentNode());
    				} else {
    					/*
    					 * Then it helps to detect DefineContainer in 
    					 * "isDefineContainer(Node sourceNode)" method. 
    					 */
    					defineContainer.add(sourceNode);
    				}
    				return creationData;
    			}
    		}
    	}
	/*
	 * Create a stub when creationData is null.
	 */
	creationData = createStub(includedFileName, (Element) sourceNode,visualDocument);
	creationData.setData(null);
	return creationData;
    }

    private String replacePattern(String origStr, String target, String replacement) {
	/*
	 * https://jira.jboss.org/jira/browse/JBIDE-4311
	 * target string could be null in some case. 
	 */
	if (null == origStr || null == target) {
	    return origStr;
	}
	    
	StringBuilder sb = new StringBuilder();
	String variable;
	String signs;
	Matcher m = WORD_PATTERN.matcher(origStr);

	// everything must be found here
	int endIndex = 0;
	int startIndex = 0;
	while (m.find()) {
	    variable = m.group(2);
	    signs = m.group(3);
	    startIndex = m.start(2);

	    if ((startIndex != 0) && (endIndex != 0)
		    && (endIndex != startIndex)) {
		sb.append(origStr.substring(endIndex, startIndex));
	    }

	    if (target.equals(variable)) {
		sb.append(replacement);
	    } else {
		sb.append(variable);
	    }
	    sb.append(signs);
	    endIndex = m.end(3);
	}

	// append the tail
	if (endIndex != origStr.length()) {
	    sb.append(origStr.substring(endIndex, origStr.length()));
	}

	if (!"".equals(sb.toString())) { //$NON-NLS-1$
	    return sb.toString();
	}
	return origStr;
    }

	private void updateNodeValue(nsIDOMNode node, Map<String, String> paramsMap) {
		Set<String> keys = paramsMap.keySet();
		if (null != node) {
			String nodeValue = node.getNodeValue();
			int matcherGroupWithVariable = 2;

			if ((null != nodeValue) && (!"".equals(nodeValue))) { //$NON-NLS-1$
				for (String key : keys) {
					Matcher curlyBracketMatcher = CURLY_BRACKET_PATTERN.matcher(nodeValue);

					String replacement = paramsMap.get(key);
					if (replacement.startsWith("#{") //$NON-NLS-1$
							&& replacement.endsWith("}")) { //$NON-NLS-1$
						// remove first 2 signs '#{'
						replacement = replacement.substring(2);
						// remove last '}' sign
						replacement = replacement.substring(0,
								replacement.length() - 1);
					}

					int lastPos = 0;
					StringBuilder sb = new StringBuilder();
					curlyBracketMatcher.reset(nodeValue);
					boolean firstFind = false;
					boolean find = curlyBracketMatcher.find();
					while (find) {
						if (!firstFind) {
							firstFind = true;
						}
						int start = curlyBracketMatcher.start(matcherGroupWithVariable);
						int end = curlyBracketMatcher.end(matcherGroupWithVariable);
						String group = replacePattern(
								curlyBracketMatcher.group(matcherGroupWithVariable),
								key, replacement);
						sb.append(nodeValue.substring(lastPos, start));
						sb.append(group);
						lastPos = end;
						find = curlyBracketMatcher.find();
					}
					if (firstFind) {
						sb.append(nodeValue.substring(lastPos,
								nodeValue.length()));
						nodeValue = sb.toString();
						node.setNodeValue(nodeValue);
					}
				}
			}
		}
	}

    private void insertParam(nsIDOMNode node, Map<String, String> paramsMap) {
    	// update current node value
    	updateNodeValue(node, paramsMap);

    	nsIDOMNamedNodeMap attributes = node.getAttributes();
    	if (null != attributes) {
    		long len = attributes.getLength();
    		for (int i = 0; i < len; i++) {
    			nsIDOMNode item = attributes.item(i);
    			// update attributes node
    			updateNodeValue(item, paramsMap);
    		}
    	}

    	nsIDOMNodeList children = node.getChildNodes();
    	if (null != children) {
    		long len = children.getLength();
    		for (int i = 0; i < len; i++) {
    			nsIDOMNode child = children.item(i);
    			// update child node
    			insertParam(child, paramsMap);
    		}
    	}
    }

    @Override
    public void validate(VpePageContext pageContext, Node sourceNode,
    		nsIDOMDocument visualDocument, VpeCreationData creationData) {

    	Map<String, String> paramsMap = new HashMap<String, String>();
    	NodeList sourceChildren = sourceNode.getChildNodes();
    	int len = sourceChildren.getLength();
    	for (int i = 0; i < len; i++) {
    		Node sourceChild = sourceChildren.item(i);
    		if (sourceChild.getNodeType() == Node.ELEMENT_NODE
    				&& "param".equals(sourceChild.getLocalName())) { //$NON-NLS-1$
    			String name = ((Element) sourceChild)
    					.getAttribute(HTML.ATTR_NAME);
    			String value = ((Element) sourceChild)
    					.getAttribute(HTML.ATTR_VALUE);
    			paramsMap.put(name, value);
    		}
    	}
    	nsIDOMNode node = creationData.getNode();
    	insertParam(node, paramsMap);

    	TemplateFileInfo templateFileInfo = (TemplateFileInfo) creationData.getData();
    	if (templateFileInfo != null) {
    		pageContext.getVisualBuilder().popIncludeStack();
    	}
    	defineContainer.remove(sourceNode);
    }

    @Override
    public void beforeRemove(VpePageContext pageContext, Node sourceNode,
    		nsIDOMNode visualNode, Object data) {
    	TemplateFileInfo templateFileInfo = (TemplateFileInfo) data;
    	if (templateFileInfo != null && templateFileInfo.templateFile != null) {
    		pageContext.getEditPart().getController().getIncludeList()
    		.removeIncludeModel(templateFileInfo.templateFile);
    	}
    }

	private void registerDefine(VpePageContext pageContext, Node defineContainer) {
		VpeTemplate template = null;
		NodeList sourceChildren = defineContainer.getChildNodes();
		int len = sourceChildren.getLength();
		for (int i = 0; i < len; i++) {
			Node sourceChild = sourceChildren.item(i);
			if (sourceChild.getNodeType() == Node.ELEMENT_NODE
					&& "define".equals(sourceChild.getLocalName())) { //$NON-NLS-1$
				if (template == null) {
					VpeTemplateManager templateManager = pageContext
							.getVisualBuilder().getTemplateManager();
					template = templateManager.getTemplate(
							pageContext, (Element) sourceChild, null);
					if (template == null) {
						break;
					}
				}
				pageContext.getVisualBuilder().registerNodes(
						new VpeElementMapping(sourceChild, null, template, null, null, null));
			}
		}
	}

	private VpeCreationData createInclude(Document sourceDocument,
			nsIDOMDocument visualDocument) {
		
		nsIDOMElement visualNewElement = visualDocument.createElement(HTML.TAG_DIV);
		VpeVisualDomBuilder.markIncludeElement(visualNewElement);
		VpeCreationData creationData = new VpeCreationData(visualNewElement);
		VpeChildrenInfo childrenInfo = new VpeChildrenInfo(visualNewElement);
		NodeList sourceChildren = sourceDocument.getChildNodes();
		int len = sourceChildren.getLength();
		for (int i = 0; i < len; i++) {
			childrenInfo.addSourceChild(sourceChildren.item(i));
		}
		creationData.addChildrenInfo(childrenInfo);
		return creationData;
	}

    @Override
    public boolean containsText() {
	return false;
    }

    public static boolean isDefineContainer(Node sourceNode) {
    	// FIX https://jira.jboss.org/jira/browse/JBIDE-3187 by sdzmitrovich
    	// TODO: it is necessary to refactor facelet templates
    	// return defineContainer.contains(sourceNode);
    	while (sourceNode != null) {
    		if (defineContainer.contains(sourceNode)) {
    			return true;
    		}
    		sourceNode = sourceNode.getParentNode();
    	}
    	return false;
    }

    protected VpeCreationData createStub(String fileName,
    		Node sourceElement, nsIDOMDocument visualDocument) {

    	nsIDOMElement container = visualDocument.createElement(HTML.TAG_DIV);
    	container.setAttribute("style", "border: 1px dashed #2A7F00"); //$NON-NLS-1$ //$NON-NLS-2$
    	VpeVisualDomBuilder.markIncludeElement(container);

    	nsIDOMElement title = visualDocument.createElement(HTML.TAG_DIV);
    	nsIDOMElement tag = visualDocument.createElement(HTML.TAG_SPAN);
    	tag.setAttribute("class", "__any__tag__caption"); //$NON-NLS-1$ //$NON-NLS-2$
    	tag.appendChild(visualDocument.createTextNode(sourceElement.getNodeName()));
    	title.appendChild(tag);
    	if (fileName != null) {
    		title.appendChild(visualDocument.createTextNode(fileName));
    	}
    	container.appendChild(title);

    	VpeCreationData creationData = new VpeCreationData(container);

    	VpeChildrenInfo childrenInfo = new VpeChildrenInfo(container);
    	NodeList sourceChildren = sourceElement.getChildNodes();
    	int len = sourceChildren.getLength();
    	for (int i = 0; i < len; i++) {
    		Node sourceChild = sourceChildren.item(i);
    		if (sourceChild.getNodeType() == Node.ELEMENT_NODE
    				&& "define".equals(sourceChild.getLocalName())) { //$NON-NLS-1$
    			childrenInfo.addSourceChild(sourceChild);
    		}
    	}
    	creationData.addChildrenInfo(childrenInfo);
    	return creationData;
    }

    static class TemplateFileInfo {
    	private IFile templateFile;

    	TemplateFileInfo(IFile templateFile) {
    		this.templateFile = templateFile;
    	}

    	public IFile getTemplateFile() {
    		return templateFile;
    	}

    	public void setTemplateFile(IFile templateFile) {
    		this.templateFile = templateFile;
    	}
    }
}
