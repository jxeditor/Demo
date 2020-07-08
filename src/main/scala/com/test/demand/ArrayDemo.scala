package com.test.demand

/**
  * @author XiaShuai on 2020/6/30.
  *         //给定一个数组 nums，编写一个函数将所有 0 移动到数组的末尾，同时保持非零元素的相对顺序。
  *         //  输入: [0,1,0,3,12]
  *         //  输出: [1,3,12,0,0]
  */
object ArrayDemo {
  def main(args: Array[String]): Unit = {
    val input = Array(2, 0, 0, 3, 1)
    // println(move(input).toList)

    val input1 = Array(9)
    // println(add(input1).toList)

    val dest = new Array[Int](3)
    Array.copy(input, 0, dest, 0, 3)
    println(input.toList)
    println(dest.toList)


    val arr = Array(2, 1, 1, 2, 5, 6)
    val test = new Array[Int](5)
    Array.copy(arr, 0, test, 0, 5)
    println(test)

    tempTest(arr)
  }

  def tempTest(nums: Array[Int]): Array[Int] = {
    val num1 = new Array[Int](nums.length - 1)
    Array.copy(nums, 0, num1, 0, nums.length - 1)

    val num2 = new Array[Int](nums.length - 1)
    Array.copy(nums, 1, num2, 0, nums.length - 1)

    println(num1.toList)
    println(num2.toList)
    num1
  }

  def move(arr: Array[Int]): Array[Int] = {
    for (j <- arr.indices.reverse) {
      if (arr(j) == 0) {
        // 等于0,将0与后面元素逐个交替放入最后
        for (i <- 0 until arr.length - j - 1) {
          arr(j + i) = arr(j + i + 1)
          arr(j + i + 1) = 0
        }
      }
    }
    arr
  }

  def add(digits: Array[Int]): Array[Int] = {
    var num = 1
    // 考虑进位问题
    for (i <- digits.indices.reverse) {
      if (digits(i) + num > 9) {
        digits(i) = num / 10 % 10
        num = num % 10
        if (i == 0) {
          return digits.+:(num)
        }
      } else {
        digits(i) = digits(i) + num
        num = 0
      }
    }
    digits
  }
}
