package org.openrndr.dokgen.sourceprocessor

import kastree.ast.Node


fun stringExpr(expr: Node.Expr): String {
    when (expr) {
        is Node.Expr.StringTmpl -> {
            return expr.elems.map {
                when (it) {
                    is Node.Expr.StringTmpl.Elem.Regular -> it.str
                    is Node.Expr.StringTmpl.Elem.ShortTmpl -> "$${it.str}"
                    else -> throw RuntimeException("unexpected string type: $it")
                }
            }.joinToString("")
        }
        else -> {
            throw RuntimeException("cannot convert expression $expr to string")
        }
    }
}

