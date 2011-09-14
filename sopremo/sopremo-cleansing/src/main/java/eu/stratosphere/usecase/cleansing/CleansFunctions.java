package eu.stratosphere.usecase.cleansing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.stratosphere.sopremo.BuiltinFunctions;
import eu.stratosphere.sopremo.EvaluationContext;
import eu.stratosphere.sopremo.TypeCoercer;
import eu.stratosphere.sopremo.aggregation.AggregationFunction;
import eu.stratosphere.sopremo.aggregation.TransitiveAggregationFunction;
import eu.stratosphere.sopremo.expressions.ComparativeExpression;
import eu.stratosphere.sopremo.jsondatamodel.ArrayNode;
import eu.stratosphere.sopremo.jsondatamodel.DoubleNode;
import eu.stratosphere.sopremo.jsondatamodel.IntNode;
import eu.stratosphere.sopremo.jsondatamodel.JsonNode;
import eu.stratosphere.sopremo.jsondatamodel.NullNode;
import eu.stratosphere.sopremo.jsondatamodel.NumericNode;
import eu.stratosphere.sopremo.jsondatamodel.TextNode;

public class CleansFunctions extends BuiltinFunctions {
	public static JsonNode length(final TextNode node) {
		return IntNode.valueOf(node.getTextValue().length());
	}
	
	private static Map<String, Class<? extends JsonNode>> typeNameToType = new HashMap<String, Class<?extends JsonNode>>();
	
	static {
		typeNameToType.put("string", TextNode.class);
		typeNameToType.put("text", TextNode.class);
		typeNameToType.put("int", IntNode.class);
	}

	public static JsonNode coerce(JsonNode input, TextNode type) {
		return  TypeCoercer.INSTANCE.coerce(input, typeNameToType.get(type));
	}

	public static JsonNode average(NumericNode... inputs) {
		double sum = 0;
		
		for (NumericNode numericNode : inputs) {
			sum += numericNode.getDoubleValue();
		}
		
		return DoubleNode.valueOf(sum / inputs.length);
	}

	public static JsonNode trim(TextNode input) {
		return TextNode.valueOf(input.getTextValue().trim());
	}
	
	public static JsonNode split(TextNode input, TextNode splitString) {
		String[] split = input.getTextValue().split(splitString.getTextValue());
		ArrayNode splitNode = new ArrayNode(null);
		for (String string : split) 
			splitNode.add(TextNode.valueOf(string));
		return splitNode;
	}
	
	public static JsonNode extract(TextNode input, TextNode pattern, JsonNode defaultValue) {
		Pattern compiledPattern = Pattern.compile(pattern.getTextValue());
		Matcher matcher = compiledPattern.matcher(input.getTextValue());

		if (!matcher.find())
			return defaultValue;

		if (matcher.groupCount() == 0)
			return TextNode.valueOf(matcher.group(0));
			
		if (matcher.groupCount() == 1)
			return TextNode.valueOf(matcher.group(1));

		ArrayNode result = new ArrayNode(null);
		for (int index = 1; index <= matcher.groupCount(); index++)
			result.add(TextNode.valueOf(matcher.group(index)));
		return result;
	}

	public static JsonNode extract(TextNode input, TextNode pattern) {
		return extract(input, pattern, NullNode.getInstance());
	}

	public static JsonNode replace(TextNode input, TextNode search, TextNode replace) {
		return TextNode.valueOf(input.getTextValue().replaceAll(search.getTextValue(), replace.getTextValue()));
	}
	
	public static JsonNode filter(ArrayNode input, JsonNode... elementsToFilter) {
		ArrayNode output = new ArrayNode(null);
		HashSet<JsonNode> filterSet = new HashSet<JsonNode>(Arrays.asList(elementsToFilter));
		for (int index = 0; index < input.size(); index++)
			if(!filterSet.contains(input.get(index)))
				output.add(input.get(index));
		return output;
	}
	
//
//	public static JsonNode group(ArrayNode array, TextNode elementName) {
//		return extract(input, pattern, NullNode.getInstance());
//	}
//	
//	public static JsonNode aggregate(ArrayNode array, EvaluationExpression groupingExpression, EvaluationExpression aggregation) {
//		final List<CompactArrayNode> nodes = new ArrayList<CompactArrayNode>();
//		for (final JsonNode jsonNode : array)
//			nodes.add(JsonUtil.asArray(groupingExpression.evaluate(jsonNode, null)));
//		Collections.sort(nodes, JsonNodeComparator.INSTANCE);
//		final ArrayNode arrayNode = new ArrayNode(null);
//		arrayNode.addAll(nodes);
//		return arrayNode;
//	}

	public static final AggregationFunction MIN = new TransitiveAggregationFunction("min", NullNode.getInstance()) {
		@Override
		protected JsonNode aggregate(final JsonNode aggregate, final JsonNode node, final EvaluationContext context) {
			if (aggregate.isNull() || ComparativeExpression.BinaryOperator.LESS.evaluate(node, aggregate))
				return node;
			return aggregate;
		}
	};

	public static final AggregationFunction MAX = new TransitiveAggregationFunction("max", NullNode.getInstance()) {
		@Override
		protected JsonNode aggregate(final JsonNode aggregate, final JsonNode node, final EvaluationContext context) {
			if (aggregate.isNull() || ComparativeExpression.BinaryOperator.LESS.evaluate(aggregate, node))
				return node;
			return aggregate;
		}
	};
}
