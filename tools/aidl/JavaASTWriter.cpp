#include "AST.h"
#include "Type.h"
#include "JavaASTWriter.h"

void
JavaASTWriter::WriteModifiers(int mod, int mask)
{
    int m = mod & mask;

    if (m & OVERRIDE) {
        fprintf(to, "@Override ");
    }

    if ((m & SCOPE_MASK) == PUBLIC) {
        fprintf(to, "public ");
    }
    else if ((m & SCOPE_MASK) == PRIVATE) {
        fprintf(to, "private ");
    }
    else if ((m & SCOPE_MASK) == PROTECTED) {
        fprintf(to, "protected ");
    }

    if (m & STATIC) {
        fprintf(to, "static ");
    }
    
    if (m & FINAL) {
        fprintf(to, "final ");
    }

    if (m & ABSTRACT) {
        fprintf(to, "abstract ");
    }
}

void
JavaASTWriter::WriteArgumentList(const vector<Expression*>& arguments)
{
    size_t N = arguments.size();
    for (size_t i=0; i<N; i++) {
        arguments[i]->Write(this);
        if (i != N-1) {
            fprintf(to, ", ");
        }
    }
}

void
JavaASTWriter::WriteField(Field* e)
{
    if (e->comment.length() != 0) {
        fprintf(to, "%s\n", e->comment.c_str());
    }
    WriteModifiers(e->modifiers, SCOPE_MASK | STATIC | FINAL | OVERRIDE);
    fprintf(to, "%s %s", e->variable->type->QualifiedName().c_str(),
            e->variable->name.c_str());
    if (e->value.length() != 0) {
        fprintf(to, " = %s", e->value.c_str());
    }
    fprintf(to, ";\n");
}

void
JavaASTWriter::WriteLiteralExpression(LiteralExpression* e)
{
    fprintf(to, "%s", e->value.c_str());
}

void
JavaASTWriter::WriteStringLiteralExpression(StringLiteralExpression* e)
{
    fprintf(to, "\"%s\"", e->value.c_str());
}

void
JavaASTWriter::WriteVariableDeclaration(Variable* e)
{
    string dim;
    for (int i=0; i<e->dimension; i++) {
        dim += "[]";
    }
    fprintf(to, "%s%s %s", e->type->QualifiedName().c_str(), dim.c_str(),
            e->name.c_str());
}

void
JavaASTWriter::WriteVariable(Variable* e)
{
    fprintf(to, "%s", e->name.c_str());
}

void
JavaASTWriter::WriteFieldVariable(FieldVariable* e)
{
    if (e->object != NULL) {
        e->object->Write(this);
    }
    else if (e->clazz != NULL) {
        fprintf(to, "%s", e->clazz->QualifiedName().c_str());
    }
    fprintf(to, ".%s", e->name.c_str());
}

void
JavaASTWriter::WriteStatementBlock(StatementBlock* e)
{
    fprintf(to, "{\n");
    int N = e->statements.size();
    for (int i=0; i<N; i++) {
        e->statements[i]->Write(this);
    }
    fprintf(to, "}\n");
}

void
JavaASTWriter::WriteExpressionStatement(ExpressionStatement* e)
{
    e->expression->Write(this);
    fprintf(to, ";\n");
}

void
JavaASTWriter::WriteAssignment(Assignment* e)
{
    e->lvalue->Write(this);
    fprintf(to, " = ");
    if (e->cast != NULL) {
        fprintf(to, "(%s)", e->cast->QualifiedName().c_str());
    }
    e->rvalue->Write(this);
}

void
JavaASTWriter::WriteMethodCall(MethodCall* e)
{
    if (e->obj != NULL) {
        e->obj->Write(this);
        fprintf(to, ".");
    }
    else if (e->clazz != NULL) {
        fprintf(to, "%s.", e->clazz->QualifiedName().c_str());
    }
    fprintf(to, "%s(", e->name.c_str());
    WriteArgumentList(e->arguments);
    fprintf(to, ")");
}

void
JavaASTWriter::WriteComparison(Comparison* e)
{
    fprintf(to, "(");
    e->lvalue->Write(this);
    fprintf(to, "%s", e->op.c_str());
    e->rvalue->Write(this);
    fprintf(to, ")");
}

void
JavaASTWriter::WriteNewExpression(NewExpression* e)
{
    fprintf(to, "new %s(", e->type->InstantiableName().c_str());
    WriteArgumentList(e->arguments);
    fprintf(to, ")");
}

void
JavaASTWriter::WriteNewArrayExpression(NewArrayExpression* e)
{
    fprintf(to, "new %s[", e->type->QualifiedName().c_str());
    e->size->Write(this);
    fprintf(to, "]");
}

void
JavaASTWriter::WriteTernary(Ternary* e)
{
    fprintf(to, "((");
    e->condition->Write(this);
    fprintf(to, ")?(");
    e->ifpart->Write(this);
    fprintf(to, "):(");
    e->elsepart->Write(this);
    fprintf(to, "))");
}

void
JavaASTWriter::WriteCast(Cast* e)
{
    fprintf(to, "((%s)", e->type->QualifiedName().c_str());
    e->expression->Write(this);
    fprintf(to, ")");
}

void
JavaASTWriter::WriteVariableDeclaration(VariableDeclaration* e)
{
    e->lvalue->WriteDeclaration(this);
    if (e->rvalue != NULL) {
        fprintf(to, " = ");
        if (e->cast != NULL) {
            fprintf(to, "(%s)", e->cast->QualifiedName().c_str());
        }
        e->rvalue->Write(this);
    }
    fprintf(to, ";\n");
}

void
JavaASTWriter::WriteIfStatement(IfStatement* e)
{
    if (e->expression != NULL) {
        fprintf(to, "if (");
        e->expression->Write(this);
        fprintf(to, ") ");
    }
    e->statements->Write(this);
    if (e->elseif != NULL) {
        fprintf(to, "else ");
        e->elseif->Write(this);
    }
}

void
JavaASTWriter::WriteReturnStatement(ReturnStatement* e)
{
    fprintf(to, "return ");
    e->expression->Write(this);
    fprintf(to, ";\n");
}

void
JavaASTWriter::WriteTryStatement(TryStatement* e)
{
    fprintf(to, "try ");
    e->statements->Write(this);
}

void
JavaASTWriter::WriteCatchStatement(CatchStatement* e)
{
    fprintf(to, "catch ");
    if (e->exception != NULL) {
        fprintf(to, "(");
        e->exception->WriteDeclaration(this);
        fprintf(to, ") ");
    }
    e->statements->Write(this);
}

void
JavaASTWriter::WriteFinallyStatement(FinallyStatement* e)
{
    fprintf(to, "finally ");
    e->statements->Write(this);
}

void
JavaASTWriter::WriteCase(Case* e)
{
    int N = e->cases.size();
    if (N > 0) {
        for (int i=0; i<N; i++) {
            string s = e->cases[i];
            if (s.length() != 0) {
                fprintf(to, "case %s:\n", s.c_str());
            } else {
                fprintf(to, "default:\n");
            }
        }
    } else {
        fprintf(to, "default:\n");
    }
    e->statements->Write(this);
}

void
JavaASTWriter::WriteSwitchStatement(SwitchStatement* e)
{
    fprintf(to, "switch (");
    e->expression->Write(this);
    fprintf(to, ")\n{\n");
    int N = e->cases.size();
    for (int i=0; i<N; i++) {
        e->cases[i]->Write(this);
    }
    fprintf(to, "}\n");
}

void
JavaASTWriter::WriteBreak(Break* e)
{
    fprintf(to, "break;\n");
}

void
JavaASTWriter::WriteMethod(Method* e)
{
    size_t N, i;

    if (e->comment.length() != 0) {
        fprintf(to, "%s\n", e->comment.c_str());
    }

    WriteModifiers(e->modifiers, SCOPE_MASK | STATIC | ABSTRACT | FINAL | OVERRIDE);

    if (e->returnType != NULL) {
        string dim;
        for (i=0; i<e->returnTypeDimension; i++) {
            dim += "[]";
        }
        fprintf(to, "%s%s ", e->returnType->QualifiedName().c_str(),
                dim.c_str());
    }
   
    fprintf(to, "%s(", e->name.c_str());

    N = e->parameters.size();
    for (i=0; i<N; i++) {
        e->parameters[i]->WriteDeclaration(this);
        if (i != N-1) {
            fprintf(to, ", ");
        }
    }

    fprintf(to, ")");

    N = e->exceptions.size();
    for (i=0; i<N; i++) {
        if (i == 0) {
            fprintf(to, " throws ");
        } else {
            fprintf(to, ", ");
        }
        fprintf(to, "%s", e->exceptions[i]->QualifiedName().c_str());
    }

    if (e->statements == NULL) {
        fprintf(to, ";\n");
    } else {
        fprintf(to, "\n");
        e->statements->Write(this);
    }
}

void
JavaASTWriter::WriteClass(Class* e)
{
    size_t N, i;

    if (e->comment.length() != 0) {
        fprintf(to, "%s\n", e->comment.c_str());
    }

    WriteModifiers(e->modifiers, ALL_MODIFIERS);

    if (e->what == Class::CLASS) {
        fprintf(to, "class ");
    } else {
        fprintf(to, "interface ");
    }

    string name = e->type->Name();
    size_t pos = name.rfind('.');
    if (pos != string::npos) {
        name = name.c_str() + pos + 1;
    }

    fprintf(to, "%s", name.c_str());

    if (e->extends != NULL) {
        fprintf(to, " extends %s", e->extends->QualifiedName().c_str());
    }

    N = e->interfaces.size();
    if (N != 0) {
        if (e->what == Class::CLASS) {
            fprintf(to, " implements");
        } else {
            fprintf(to, " extends");
        }
        for (i=0; i<N; i++) {
            fprintf(to, " %s", e->interfaces[i]->QualifiedName().c_str());
        }
    }

    fprintf(to, "\n");
    fprintf(to, "{\n");

    N = e->elements.size();
    for (i=0; i<N; i++) {
        e->elements[i]->Write(this);
    }

    fprintf(to, "}\n");

}

static string
escape_backslashes(const string& str)
{
    string result;
    const size_t I=str.length();
    for (size_t i=0; i<I; i++) {
        char c = str[i];
        if (c == '\\') {
            result += "\\\\";
        } else {
            result += c;
        }
    }
    return result;
}

void
JavaASTWriter::WriteDocument(Document* e)
{
    size_t N, i;

    if (e->comment.length() != 0) {
        fprintf(to, "%s\n", e->comment.c_str());
    }
    fprintf(to, "/*\n"
                " * This file is auto-generated.  DO NOT MODIFY.\n"
                " * Original file: %s\n"
                " */\n", escape_backslashes(e->originalSrc).c_str());
    if (e->package.length() != 0) {
        fprintf(to, "package %s;\n", e->package.c_str());
    }

    N = e->classes.size();
    for (i=0; i<N; i++) {
        Class* c = e->classes[i];
        c->Write(this);
    }
}

