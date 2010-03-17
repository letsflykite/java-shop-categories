package org.neo4j.examples.shopcategories;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;

public class ShopCategoriesAppTest
{
    private static ShopCategoriesService service;

    @BeforeClass
    public static void setup()
    {
        service = new ShopCategoriesServiceImpl();
        setupDb();
    }

    @AfterClass
    public static void teardown()
    {
        cleanDb();
    }

    private static void setupDb()
    {
        service.beginTx();

        Category electronics = service.createCategory( "Electronics", null );
        Category cameras = service.createCategory( "Cameras", electronics );
        Category computers = service.createCategory( "Computers", electronics );

        Category desktops = service.createCategory( "Desktops", computers );
        Category laptops = service.createCategory( "Laptops", computers );

        AttributeType weight = service.createAttributeType( "weight" );
        AttributeType count = service.createAttributeType( "count" );
        AttributeType length = service.createAttributeType( "length" );
        AttributeType frequency = service.createAttributeType( "frequency" );
        AttributeType name = service.createAttributeType( "name" );

        final AttributeDefinition aName = electronics.createAttributeDefinition(
                name, "name" );
        aName.setRequired( true );
        final AttributeDefinition aWeight = electronics.createAttributeDefinition(
                weight, "weight" );
        aWeight.setRequired( true );
        final AttributeDefinition aShippingWeight = electronics.createAttributeDefinition(
                weight, "shipping weight" );
        aShippingWeight.setRequired( true );
        final AttributeDefinition aCpuFreq = computers.createAttributeDefinition(
                frequency, "cpu frequency" );
        aCpuFreq.setRequired( true );
        AttributeDefinition aExpansionSlots = desktops.createAttributeDefinition(
                count, "expansion slots" );
        aExpansionSlots.setDefaultValue( 4 );
        AttributeDefinition aDisplaySize = laptops.createAttributeDefinition(
                length, "display size" );
        aDisplaySize.setDefaultValue( 15.0 );

        service.createProduct( desktops,
                new HashMap<AttributeDefinition, Object>()
                {
                    {
                        put( aName, "Dell Desktop" );
                        put( aWeight, 17.1 );
                        put( aShippingWeight, 22.3 );
                        put( aCpuFreq, 3000 );
                    }
                } );
        service.createProduct( laptops,
                new HashMap<AttributeDefinition, Object>()
                {
                    {
                        put( aName, "HP Laptop" );
                        put( aWeight, 3.5 );
                        put( aShippingWeight, 6.3 );
                        put( aCpuFreq, 2000 );
                    }
                } );
        service.commitTx();
        service.beginTx();
        for ( Product product : computers.getAllProducts() )
        {
            System.out.println( product );
        }

        service.commitTx();
    }

    private static void cleanDb()
    {
        service.beginTx();
        Traverser traverser = ( (CategoryImpl) service.getRootCategory() ).getUnderlyingContainer().traverse(
                Order.DEPTH_FIRST, StopEvaluator.END_OF_GRAPH,
                ReturnableEvaluator.ALL, RelationshipTypes.ATTRIBUTE,
                Direction.BOTH, RelationshipTypes.PRODUCT, Direction.BOTH,
                RelationshipTypes.SUBCATEGORY, Direction.BOTH );
        for ( Node node : traverser )
        {
            for ( Relationship rel : node.getRelationships() )
            {
                rel.delete();
            }
            node.delete();
        }
        service.commitTx();
    }

    @Before
    public void start()
    {
        service.beginTx();
    }

    @After
    public void end()
    {
        service.rollbackTx();
    }

    @Test( expected = IllegalArgumentException.class )
    public void productWithMissingRequiredAttributes()
    {
        service.createProduct( service.getRootCategory(),
                new HashMap<AttributeDefinition, Object>() );
    }

    @Test
    public void testNumberOfProducts()
    {
        int count = 0;
        for ( Product product : service.getRootCategory().getAllProducts() )
        {
            count++;
        }
        assertEquals( 2, count );
    }
}
