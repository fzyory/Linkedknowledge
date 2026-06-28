package com.LinkedKnowledge.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Java 集合框架学习 - D1
 * 通过实际代码理解 List, Set, Map 的特性和区别
 */
public class CollectionDemo {

    public static void main(String[] args) {
        System.out.println("=== Java 集合框架学习 ===\n");

        // 实验 1：ArrayList 的基本特性
        testArrayList();

        // 实验 2：HashSet 的基本特性
        testHashSet();
    }

    /**
     * 实验 1：ArrayList - 有序、可重复的列表
     */
    private static void testArrayList() {
        System.out.println("【实验 1：ArrayList】");

        // 创建一个存储学生姓名的列表
        List<String> students = new ArrayList<>();

        // 1. 添加元素
        students.add("张三");
        students.add("李四");
        students.add("王五");
        students.add("张三");  // 注意：可以添加重复元素
        System.out.println("添加后的列表：" + students);

        // 2. 获取元素（按索引）
        System.out.println("第 1 个学生：" + students.get(0));  // 索引从 0 开始
        System.out.println("第 2 个学生：" + students.get(1));

        // 3. 列表大小
        System.out.println("班级人数：" + students.size());

        // 4. 检查是否包含某个元素
        System.out.println("张三在班级里吗？" + students.contains("张三"));
        System.out.println("赵六在班级里吗？" + students.contains("赵六"));

        // 5. 删除元素
        students.remove("李四");  // 按值删除
        System.out.println("李四转学后：" + students);

        // 6. 遍历列表
        System.out.println("遍历所有学生：");
        for (String student : students) {
            System.out.println("  - " + student);
        }

        System.out.println("\n观察：");
        System.out.println("1. ArrayList 是有序的（保持添加顺序）");
        System.out.println("2. ArrayList 可以有重复元素（张三出现了 2 次）");
        System.out.println("3. 可以通过索引访问元素 get(0), get(1)...");
        System.out.println();
    }

    /**
     * 实验 2：HashSet - 无序、不可重复的集合
     */
    private static void testHashSet() {
        System.out.println("【实验 2：HashSet】");

        // 创建一个存储学生姓名的集合
        Set<String> students = new HashSet<>();

        // 1. 添加元素（和 ArrayList 用同样的数据）
        students.add("张三");
        students.add("李四");
        students.add("王五");
        students.add("张三");  // 注意：尝试添加重复元素
        System.out.println("添加后的集合：" + students);

        // 2. 集合大小
        System.out.println("班级人数：" + students.size());

        // 3. 检查是否包含某个元素
        System.out.println("张三在班级里吗？" + students.contains("张三"));

        // 4. 删除元素
        students.remove("李四");
        System.out.println("李四转学后：" + students);

        // 5. 遍历集合
        System.out.println("遍历所有学生：");
        for (String student : students) {
            System.out.println("  - " + student);
        }

        // 注意：HashSet 没有 get(index) 方法！
        // students.get(0);  // 这行代码会报错

        System.out.println("\n观察：");
        System.out.println("1. HashSet 是无序的（输出顺序可能和添加顺序不同）");
        System.out.println("2. HashSet 不允许重复（张三只出现 1 次）");
        System.out.println("3. 不能通过索引访问元素（没有 get 方法）");
        System.out.println();
    }
}
