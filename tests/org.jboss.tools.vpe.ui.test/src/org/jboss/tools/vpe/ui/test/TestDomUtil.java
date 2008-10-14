/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.ui.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.common.model.util.XMLUtil;
import org.jboss.tools.vpe.editor.util.Constants;
import org.jboss.tools.vpe.editor.util.HTML;
import org.mozilla.interfaces.nsIDOMAttr;
import org.mozilla.interfaces.nsIDOMNamedNodeMap;
import org.mozilla.interfaces.nsIDOMNode;
import org.mozilla.interfaces.nsIDOMNodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Sergey Dzmitrovich
 * 
 */
public class TestDomUtil {

	final public static String ID_ATTRIBUTE = "id"; //$NON-NLS-1$

	final public static String ILLEGAL_ATTRIBUTES = "illegalAttributes"; //$NON-NLS-1$

	final public static String ILLEGAL_ATTRIBUTES_SEPARATOR = Constants.COMMA;

	final public static String START_REGEX = "{"; //$NON-NLS-1$

	final public static String END_REGEX = "}"; //$NON-NLS-1$

	public static Document getDocument(File file) throws FileNotFoundException {
		// create reader
		FileReader reader = new FileReader(file);

		// return document
		return XMLUtil.getDocument(reader);
	}

	public static Document getDocument(String content)
			throws FileNotFoundException {
		// create reader
		StringReader reader = new StringReader(content);

		// return document
		return XMLUtil.getDocument(reader);
	}

	/**
	 * 
	 * @param document
	 * @param elementId
	 * @return
	 */
	public static Element getElemenById(Document document, String elementId) {

		Element element = document.getDocumentElement();

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ((child.getNodeType() == Node.ELEMENT_NODE)
					&& elementId.equals(((Element) child)
							.getAttribute(ID_ATTRIBUTE)))
				return (Element) child;

		}

		return null;

	}

	/**
	 * 
	 * @param element
	 * @return
	 */
	public static Element getFirstChildElement(Element element) {

		if (element != null) {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);

				if (child.getNodeType() == Node.ELEMENT_NODE)
					return (Element) child;

			}
		}
		return null;

	}

	/**
	 * 
	 * @param vpeNode
	 * @param schemeNode
	 * @return
	 * @throws ComparisonException
	 */
	public static void compareNodes(nsIDOMNode vpeNode, Node modelNode)
			throws ComparisonException {

		if (!modelNode.getNodeName().equalsIgnoreCase(vpeNode.getNodeName())) {
			throw new ComparisonException("name of tag is \""
					+ vpeNode.getNodeName() + "\"but must be \""
					+ modelNode.getNodeName() + "\"");
		}
		if ((modelNode.getNodeValue() != null)
				&& (!modelNode.getNodeValue().trim().equalsIgnoreCase(
						vpeNode.getNodeValue().trim()))) {
			throw new ComparisonException("value of " + vpeNode.getNodeName()
					+ " is \"" + vpeNode.getNodeValue().trim()
					+ "\" but must be \"" + modelNode.getNodeValue().trim()
					+ "\"");
		}
		// compare node's attributes
		if (modelNode.getNodeType() == Node.ELEMENT_NODE) {

			compareAttributes(modelNode.getAttributes(), vpeNode
					.getAttributes());
		}

		// compare children
		nsIDOMNodeList vpeChildren = vpeNode.getChildNodes();
		NodeList schemeChildren = modelNode.getChildNodes();
		int realCount = 0;
		for (int i = 0; i < schemeChildren.getLength(); i++) {

			Node schemeChild = schemeChildren.item(i);

			// leave out empty text nodes in test dom model
			if ((schemeChild.getNodeType() == Node.TEXT_NODE)
					&& ((schemeChild.getNodeValue() == null) || (schemeChild
							.getNodeValue().trim().length() == 0)))
				continue;

			nsIDOMNode vpeChild = vpeChildren.item(realCount++);

			// leave out empty text nodes in vpe dom model
			while (((vpeChild.getNodeType() == Node.TEXT_NODE) && ((vpeChild
					.getNodeValue() == null) || (vpeChild.getNodeValue().trim()
					.length() == 0)))) {
				vpeChild = vpeChildren.item(realCount++);

			}

			compareNodes(vpeChild, schemeChild);

		}

	}

	/**
	 * get ids of tests
	 * 
	 * @param testDocument
	 * @return
	 */
	public static List<String> getTestIds(Document testDocument) {
		Element rootElement = testDocument.getDocumentElement();
		List<String> ids = new ArrayList<String>();
		NodeList children = rootElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE)
				ids.add(((Element) child).getAttribute(ID_ATTRIBUTE));

		}
		return ids;
	}

	private static void compareAttributes(NamedNodeMap modelAttributes,
			nsIDOMNamedNodeMap vpeAttributes) throws ComparisonException {

		for (int i = 0; i < modelAttributes.getLength(); i++) {
			Attr modelAttr = (Attr) modelAttributes.item(i);
			String name = modelAttr.getName();

			// if there are limitation of attributes
			if (ILLEGAL_ATTRIBUTES.equals(name)) {

				String[] illegalAttributes = modelAttr.getNodeValue().split(
						ILLEGAL_ATTRIBUTES_SEPARATOR);

				for (String illegalAttributeName : illegalAttributes) {
					if (vpeAttributes.getNamedItem(illegalAttributeName.trim()) != null)
						throw new ComparisonException("illegal attribute :"
								+ illegalAttributeName);
				}

			} else {

				nsIDOMAttr vpeAttr = (nsIDOMAttr) vpeAttributes.getNamedItem(
						name).queryInterface(nsIDOMAttr.NS_IDOMATTR_IID);

				if (vpeAttr == null)
					throw new ComparisonException("there is not : \"" + name
							+ "\" attribute");

				if (HTML.ATTR_STYLE.equalsIgnoreCase(name)) {

					String[] modelParameters = modelAttr.getNodeValue().split(
							Constants.SEMICOLON);
					String[] vpeParameters = vpeAttr.getNodeValue().split(
							Constants.SEMICOLON);

					for (int j = 0; j < modelParameters.length; j++) {
						String modelParam = modelParameters[j];
						String vpeParam = vpeParameters[j];

						String[] splittedModelParam = modelParam.split(
								Constants.COLON, 2);

						String[] splittedVpeParam = vpeParam.split(
								Constants.COLON, 2);

						if (!splittedModelParam[0].trim().equals(
								splittedVpeParam[0].trim())) {
							throw new ComparisonException(
									"param of style attribute is\""
											+ splittedVpeParam[0].trim()
											+ "\" but must be \""
											+ splittedModelParam[0].trim()
											+ "\"");
						}

						compareComplexStrings(splittedModelParam[1].trim(),
								splittedVpeParam[1].trim());

					}

				} else {

					compareComplexStrings(modelAttr.getNodeValue().trim(),
							vpeAttr.getNodeValue().trim());

				}
			}
		}
	}

	static private void compareComplexStrings(String modelString,
			String vpeString) throws ComparisonException {

		if (modelString.startsWith(START_REGEX)
				&& modelString.endsWith(END_REGEX)) {

			String regex = modelString.substring(START_REGEX.length(),
					modelString.length() - END_REGEX.length());

			Matcher matcher = Pattern.compile(regex).matcher(vpeString);
			if (!matcher.find()) {
				throw new ComparisonException("string is\"" + vpeString
						+ "\" but pattern is \"" + regex + "\"");
			}

		} else if (!modelString.equals(vpeString)) {
			throw new ComparisonException("string is\"" + vpeString
					+ "\" but must be \"" + modelString + "\"");
		}

	}

	/**
	 * is created to be sure that attributes/parameters will be correctly
	 * compared ( ignore case )
	 * 
	 * @param list
	 * @param string
	 * @return
	 */
	static private boolean findIgnoreCase(String[] strings,
			String requiredString) {

		for (String string : strings) {

			if (string.equalsIgnoreCase(requiredString))
				return true;

		}

		return false;
	}

}
