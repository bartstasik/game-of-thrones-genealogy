package com.got.genealogy.core.processor;

import com.got.genealogy.core.family.FamilyTree;
import com.got.genealogy.core.family.person.Gender;
import com.got.genealogy.core.family.person.Person;
import com.got.genealogy.core.family.person.Relation;
import com.got.genealogy.core.family.person.Relationship;

import java.util.ArrayList;
import java.util.List;

import static com.got.genealogy.core.family.person.Relationship.*;
import static com.got.genealogy.core.processor.data.File.exportGVFile;
import static com.got.genealogy.core.processor.data.File.exportSortedFile;
import static com.got.genealogy.core.processor.data.File.loadFile;
import static com.got.genealogy.core.processor.data.InformationPool.*;
import static com.got.genealogy.core.processor.data.StringUtils.toTitleCase;

public class Genealogy {

    private static List<FamilyTree> families = new ArrayList<>();

    public static FamilyTree getFamily(String familyName) {
        for (FamilyTree family : families) {
            if (family.getLabel()
                    .toUpperCase()
                    .equals(familyName.toUpperCase())) {
                return family;
            }
        }
        return null;
    }

    public static FamilyTree getFamily(int familyIndex) {
        return families.get(familyIndex);
    }

    public static void setFamily(int familyIndex, FamilyTree familyTree) {
        families.set(familyIndex, familyTree);
    }

    public static void addFamily(FamilyTree familyTree) {
        families.add(familyTree);
    }

    public static void removeFamily(String familyName) {
        FamilyTree family = getFamily(familyName);
        if (family != null) {
            removeFamily(family);
        }
    }

    public static void removeFamily(int familyIndex) {
        families.remove(familyIndex);
    }

    public static void removeFamily(FamilyTree familyTree) {
        families.remove(familyTree);
    }

    public static FamilyTree loadRelation(String absolutePath, String familyName) {
        try {
            String[][] file = loadFile(absolutePath);
            FamilyTree family = getFamily(familyName);

            if (file == null) {
                return null;
            }

            if (family == null) {
                family = new FamilyTree(familyName);
                addFamily(family);
            }

            for (String[] row : file) {
                String name1, name2;
                Gender gender;
                Relationship relationship;

                switch (row.length) {
                    case 1:
                        name1 = row[0];
                        family.addPerson(name1);
                        break;
                    case 2:
                        name1 = row[0];
                        gender = getGender(row[1]);
                        family.addPerson(name1, gender);
                        break;
                    case 3:
                        name1 = row[0];
                        name2 = row[2];
                        gender = getGender(row[1]);
                        relationship = getRelationship(row[1]);

                        if (relationship != null) {
                            family.addPerson(name1, gender);
                            family.addPerson(name2);
                            family.addRelation(name1, name2, relationship);
                        } else {
                            family.addPerson(name1);
                            family.addPerson(name2);
                            family.addExtraRelation(name1, name2, row[1]);
                        }
                        break;
                    default:
                        System.out.println("INCORRECT FILE FORMAT");
                        return null;
                }
            }

            return family;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[] exportDOT(String absolutePath, String familyName) {
        FamilyTree family = getFamily(familyName);
        return family == null ? null : exportGVFile(absolutePath, family);
    }

    public static boolean exportSorted(String absolutePath, String familyName) {
        FamilyTree family = getFamily(familyName);
        return family != null && exportSortedFile(absolutePath, family);
    }

    public static String[] findRelationship(String name1, String name2, String familyName) {
        FamilyTree family = getFamily(familyName);
        if (family == null) {
            return null;
        }

        Person person1 = family.getPerson(name1);
        Person person2 = family.getPerson(name2);

        if (person1 == person2 || person1 == null || person2 == null){
            return null;
        }

        return processRelationship(person1, person2, family);
    }

    private static String[] processRelationship(Person person1, Person person2, FamilyTree family) {
        int x, y, z, p, grandTimes, cousinTimes;
        int[] coordinates;
        String relationship;

        List<String> relationships = new ArrayList<>();

        Relation direct = family.getRelation(person1, person2);
        Gender gender = person1.getGender();
        boolean inLaw = false;

        if (direct != null) {
            if (direct.getExtras().size() > 0) {
                // Get all extra relations,
                // but still needs to search
                // if any distant family relation.
                for (String extra : direct.getExtras()) {
                    relationships.add(toTitleCase(extra));
                };
            } else {
                relationship = getRelationship(gender, direct);
                if (!relationship.isEmpty()) {
                    return finalRelationship(relationships, relationship);
                }
            }
        }

        coordinates = family.calculateRelationCoords(
                person1,
                person2);

        if (coordinates != null) {
            x = coordinates[0];
            y = coordinates[1];
            z = coordinates[2];
            p = coordinates[3];

            // if (y < 0) x = x + y
            // Gets lowest common
            // ancestor.
            x = y < 0 ? x + y : x;

            if (z > 0) {
                return finalRelationship(relationships,
                        "Not Blood-Related");
            }
            if (p > 1) {
                return finalRelationship(relationships,
                        "Not Related, but someone in family is married to their relative");
            } else if (p == 1) {
                inLaw = true;
            }

            // x == 0 && y < 0      :: n-PARENT
            // x == 1 && y < 0      :: n-AUNT_OR_UNCLE
            // x >= 1 && y == 0     :: SIBLING or n-COUSIN
            // x == y && y == 0      :: SPOUSE
            // x == y && y > 0      :: n-CHILD
            // x >= 2 && y == x - 1 :: n-NIECE_OR_NEPHEW
            // x >= 2 && y < 0      :: DESCENDANT_COUSIN, x-REMOVED
            // x >= y + 2 && y > 0  :: ASCENDANT_COUSIN, y-REMOVED

            if (isParent(x, y)) {
                // Linear: positive of Y
                // Constant: x
                y *= -1;
                if (y > 3) {
                    return finalRelationship(
                            relationships,
                            countRepeatedTimes(y - 2, inLaw, gender, GREAT_GRANDPARENT));
                }
                return finalRelationship(
                        relationships,
                        countInLaw(inLaw, gender, PARENT_COORDS[y - 1]));
            }
            if (isAuntUncle(x, y)) {
                // Linear: positive of y
                // Constant: x
                y *= -1;
                if (y > 3) {
                    return finalRelationship(
                            relationships,
                            countRepeatedTimes(y - 2, inLaw, gender, GREAT_GRANDAUNT_OR_UNCLE));
                }
                return finalRelationship(
                        relationships,
                        countInLaw(inLaw, gender, AUNT_UNCLE_COORDS[y - 1]));
            }
            if (isSiblingCousin(x, y)) {
                // Linear: x
                // Constant: y
                cousinTimes = x - 1;
                if (x > 2) {
                    return finalRelationship(
                            relationships,
                            countRepeatedTimes(x - 1, inLaw, gender, COUSIN));
                }
                return finalRelationship(
                        relationships,
                        countInLaw(inLaw, gender, SIBLING_COORDS[x - 1]));
            }
            if (isSpouse(x, y)) {
                // Constant: x
                // Constant: y
                return finalRelationship(
                        relationships,
                        getRelationship(gender, SPOUSE));
            }
            if (isChild(x, y)) {
                // Linear: x
                // Linear: y
                if (y > 3) {
                    return finalRelationship(
                            relationships,
                            countRepeatedTimes(y - 2, inLaw, gender, GREAT_GRANDCHILD));
                }
                return finalRelationship(
                        relationships,
                        countInLaw(inLaw, gender, CHILD_COORDS[x - 1]));
            }
            if (isNieceNephew(x, y)) {
                // Linear: x
                // Linear: y
                return finalRelationship(
                        relationships,
                        countInLaw(inLaw, gender, NIECE_NEPHEW_COORDS[x - 2]));
            }
            if (isDescCousin(x, y)) {
                // Linear: x
                // Linear: positive of y
                return finalRelationship(
                        relationships,
                        countRepeatedTimes(x - 1, inLaw, gender, DESCENDANT_COUSIN));
            }
            if (isAscCousin(x, y)) {
                return finalRelationship(
                        relationships,
                        countRepeatedTimes(x - 2, inLaw, gender, ASCENDANT_COUSIN));
            }
        }
        return finalRelationship(relationships, "Not Related");
    }

    private static boolean isParent(int x, int y) {
        return x == 0 && y < 0;
    }
    private static boolean isAuntUncle(int x, int y) {
        return x == 1 && y < 0;
    }
    private static boolean isSiblingCousin(int x, int y) {
        return x >= 1 && y == 0;
    }
    private static boolean isSpouse(int x, int y) {
        return x == y && y == 0;
    }
    private static boolean isChild(int x, int y) {
        return x == y && y > 0;
    }
    private static boolean isNieceNephew(int x, int y) {
        return x >= 2 && y == x - 1;
    }
    private static boolean isDescCousin(int x, int y) {
        return x >= 2 && y < 0;
    }
    private static boolean isAscCousin(int x, int y) {
        return x >= y + 2 && y > 0;
    }

    private static String countRepeatedTimes(int count,
                                             boolean inLaw,
                                             Gender gender,
                                             Relationship relationship){
        return count + "x " + countInLaw(inLaw, gender, relationship);
    }

    private static String countInLaw(boolean inLaw,
                                     Gender gender,
                                     Relationship relationship) {
        String lawString = !inLaw ? "" : " In-Law";
        return getRelationship(gender, relationship) + lawString;
    }

    private static String[] finalRelationship(List<String> relationships,
                                              String relationship) {
        relationships.add(relationship);
        return relationships.toArray(new String[0]);
    }
}
