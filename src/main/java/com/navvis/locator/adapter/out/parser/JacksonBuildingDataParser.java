package com.navvis.locator.adapter.out.parser;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.navvis.locator.domain.model.geometry.*;
import com.navvis.locator.domain.port.out.BuildingDataParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JacksonBuildingDataParser implements BuildingDataParser {

    private final ObjectMapper objectMapper;

    public JacksonBuildingDataParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Building> parse(byte[] fileContent) {
        JsonNode root = objectMapper.readTree(fileContent);
        List<Building> buildings = new ArrayList<>();

        for (JsonNode node : root) {
            buildings.add(parseBuilding(node));
        }

        return buildings;
    }

    private Building parseBuilding(JsonNode node) {
        String name = node.get("name").asText();
        Polygon2D outline = parsePolygon(node.get("outline"));
        HeightRange height = parseHeight(node.get("height"));

        List<Floor> floors = new ArrayList<>();
        for (JsonNode floorNode : node.get("floors")) {
            floors.add(parseFloor(floorNode));
        }

        return new Building(name, outline, height, floors);
    }

    private Floor parseFloor(JsonNode node) {
        String name = node.get("name").asText();
        Polygon2D outline = parsePolygon(node.get("outline"));
        HeightRange height = parseHeight(node.get("height"));

        return new Floor(name, outline, height);
    }

    private Polygon2D parsePolygon(JsonNode node) {
        List<Point2D> vertices = new ArrayList<>();

        for (JsonNode pair : node) {
            double x = pair.get(0).asDouble();
            double y = pair.get(1).asDouble();
            vertices.add(new Point2D(x, y));
        }

        return new Polygon2D(vertices);
    }

    private HeightRange parseHeight(JsonNode node) {
        double min = node.get(0).asDouble();
        double max = node.get(1).asDouble();
        return new HeightRange(min, max);
    }
}